package com.example.schoolforum.service.impl;

import com.example.schoolforum.enums.PointsType;
import com.example.schoolforum.enums.UserLevel;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.PointsRecordMapper;
import com.example.schoolforum.mapper.SignRecordMapper;
import com.example.schoolforum.mapper.UsersMapper;
import com.example.schoolforum.pojo.PointsRecord;
import com.example.schoolforum.pojo.SignRecord;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.pojo.dto.*;
import com.example.schoolforum.service.SignService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static com.example.schoolforum.pojo.table.UsersTableDef.USERS;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignServiceImpl extends ServiceImpl<SignRecordMapper, SignRecord> implements SignService {

    private final UsersMapper usersMapper;
    private final SignRecordMapper signRecordMapper;
    private final PointsRecordMapper pointsRecordMapper;

    private static final Integer SIGN_CARD_COST = 50;
    private static final Integer BASE_EXP = 10;
    private static final Integer BASE_POINTS = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SignResponse sign(Long userId) {
        Users user = usersMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        LocalDate today = LocalDate.now();
        log.info("签到调试 - userId: {}, today: {}, lastSignDate: {}, continuousSignDays: {}, exp: {}, points: {}",
                userId, today, user.getLastSignDate(), user.getContinuousSignDays(), user.getExp(), user.getPoints());

        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SignRecord::getUserId).eq(userId)
                .and(SignRecord::getSignDate).eq(today);
        SignRecord existingRecord = signRecordMapper.selectOneByQuery(queryWrapper);
        if (existingRecord != null) {
            throw new BusinessException("今日已签到");
        }

        Integer continuousDays = calculateContinuousDays(userId, today);
        log.info("签到调试 - 计算出的连续天数: {}, yesterday: {}", continuousDays, today.minusDays(1));

        int expGained = calculateExpReward(continuousDays);
        int pointsGained = calculatePointsReward(continuousDays);

        SignRecord signRecord = SignRecord.builder()
                .userId(userId)
                .signDate(today)
                .expGained(expGained)
                .pointsGained(pointsGained)
                .continuousDays(continuousDays)
                .isRepair(false)
                .createdAt(LocalDateTime.now())
                .build();
        try {
            signRecordMapper.insert(signRecord);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("今日已签到");
        }

        log.info("签到调试 - 准备更新用户: expGained={}, pointsGained={}, continuousDays={}",
                expGained, pointsGained, continuousDays);

        Users updateUser = UpdateEntity.of(Users.class, userId);
        updateUser.setContinuousSignDays(continuousDays);
        updateUser.setLastSignDate(today);
        UpdateWrapper<Users> wrapper = UpdateWrapper.of(updateUser);
        wrapper.set(USERS.EXP, USERS.EXP.add(expGained));
        wrapper.set(USERS.POINTS, USERS.POINTS.add(pointsGained));
        boolean updated = usersMapper.update(updateUser) > 0;
        log.info("签到调试 - 更新用户结果: updated={}", updated);

        Users updatedUser = usersMapper.selectOneById(userId);
        int newExp = updatedUser.getExp() != null ? updatedUser.getExp() : 0;
        int newPoints = updatedUser.getPoints() != null ? updatedUser.getPoints() : 0;
        Integer newLevel = UserLevel.calculateLevel(newExp);
        
        Users levelUpdate = UpdateEntity.of(Users.class, userId);
        levelUpdate.setLevel(newLevel);
        usersMapper.update(levelUpdate);

        PointsRecord pointsRecord = PointsRecord.builder()
                .userId(userId)
                .changeAmount(pointsGained)
                .balanceAfter(newPoints)
                .type(PointsType.SIGN)
                .description("每日签到，连续" + continuousDays + "天")
                .createdAt(LocalDateTime.now())
                .build();
        pointsRecordMapper.insert(pointsRecord);

        Integer oldLevel = UserLevel.calculateLevel(user.getExp() != null ? user.getExp() : 0);
        boolean levelUp = newLevel > oldLevel;

        return SignResponse.builder()
                .success(true)
                .message("签到成功")
                .expGained(expGained)
                .pointsGained(pointsGained)
                .currentExp(newExp)
                .currentLevel(newLevel)
                .currentPoints(newPoints)
                .continuousDays(continuousDays)
                .levelUp(levelUp)
                .newLevel(levelUp ? newLevel : null)
                .build();
    }

    @Override
    public SignStatusResponse getSignStatus(Long userId) {
        Users user = usersMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        LocalDate today = LocalDate.now();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SignRecord::getUserId).eq(userId)
                .and(SignRecord::getSignDate).eq(today);
        SignRecord todayRecord = signRecordMapper.selectOneByQuery(queryWrapper);

        YearMonth currentMonth = YearMonth.now();
        QueryWrapper monthQuery = QueryWrapper.create()
                .where(SignRecord::getUserId).eq(userId)
                .and(SignRecord::getSignDate).ge(currentMonth.atDay(1))
                .and(SignRecord::getSignDate).le(currentMonth.atEndOfMonth());
        long monthSignDays = signRecordMapper.selectCountByQuery(monthQuery);

        Integer expToNextLevel = UserLevel.getExpToNextLevel(user.getExp());

        return SignStatusResponse.builder()
                .todaySigned(todayRecord != null)
                .continuousDays(user.getContinuousSignDays())
                .level(user.getLevel())
                .exp(user.getExp())
                .points(user.getPoints())
                .signCards(user.getSignCards())
                .expToNextLevel(expToNextLevel)
                .lastSignDate(user.getLastSignDate())
                .monthSignDays((int) monthSignDays)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairSignResponse repairSign(Long userId, LocalDate signDate) {
        Users user = usersMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        LocalDate today = LocalDate.now();
        if (!signDate.isBefore(today) || signDate.isBefore(today.minusMonths(2))) {
            throw new BusinessException("只能补签近2个月内的日期（不含今天）");
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SignRecord::getUserId).eq(userId)
                .and(SignRecord::getSignDate).eq(signDate);
        SignRecord existingRecord = signRecordMapper.selectOneByQuery(queryWrapper);
        if (existingRecord != null) {
            throw new BusinessException("该日期已签到");
        }

        if (user.getSignCards() <= 0) {
            throw new BusinessException("补签卡不足，请先兑换");
        }

        int expGained = BASE_EXP / 2;
        int pointsGained = BASE_POINTS / 2;

        Integer continuousDays = calculateRepairContinuousDays(userId, signDate);

        SignRecord signRecord = SignRecord.builder()
                .userId(userId)
                .signDate(signDate)
                .expGained(expGained)
                .pointsGained(pointsGained)
                .continuousDays(continuousDays)
                .isRepair(true)
                .createdAt(LocalDateTime.now())
                .build();
        signRecordMapper.insert(signRecord);

        recalculateContinuousDaysAfterRepair(userId, signDate);

        Integer newContinuousDays = calculateUserContinuousDays(userId, today);
        LocalDate newLastSignDate = calculateUserLastSignDate(userId);

        Users updateUser = UpdateEntity.of(Users.class, userId);
        updateUser.setContinuousSignDays(newContinuousDays);
        updateUser.setLastSignDate(newLastSignDate);
        UpdateWrapper<Users> wrapper = UpdateWrapper.of(updateUser);
        wrapper.set(USERS.EXP, USERS.EXP.add(expGained));
        wrapper.set(USERS.POINTS, USERS.POINTS.add(pointsGained));
        wrapper.set(USERS.SIGN_CARDS, USERS.SIGN_CARDS.add(-1));
        usersMapper.update(updateUser);

        Users updatedUser = usersMapper.selectOneById(userId);
        int newExp = updatedUser.getExp() != null ? updatedUser.getExp() : 0;
        int newPoints = updatedUser.getPoints() != null ? updatedUser.getPoints() : 0;
        int newSignCards = updatedUser.getSignCards() != null ? updatedUser.getSignCards() : 0;
        Integer newLevel = UserLevel.calculateLevel(newExp);
        
        Users levelUpdate = UpdateEntity.of(Users.class, userId);
        levelUpdate.setLevel(newLevel);
        usersMapper.update(levelUpdate);

        PointsRecord pointsRecord = PointsRecord.builder()
                .userId(userId)
                .changeAmount(pointsGained)
                .balanceAfter(newPoints)
                .type(PointsType.SIGN)
                .description("补签奖励")
                .createdAt(LocalDateTime.now())
                .build();
        pointsRecordMapper.insert(pointsRecord);

        return RepairSignResponse.builder()
                .success(true)
                .message("补签成功，获得 " + expGained + " 经验，" + pointsGained + " 积分")
                .expGained(expGained)
                .pointsGained(pointsGained)
                .remainingCards(newSignCards)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExchangeSignCardResponse exchangeSignCard(Long userId) {
        Users user = usersMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Users updateUser = UpdateEntity.of(Users.class, userId);
        UpdateWrapper<Users> wrapper = UpdateWrapper.of(updateUser);
        wrapper.set(USERS.POINTS, USERS.POINTS.add(-SIGN_CARD_COST));
        wrapper.set(USERS.SIGN_CARDS, USERS.SIGN_CARDS.add(1));
        int rows = usersMapper.updateByCondition(updateUser, USERS.ID.eq(userId).and(USERS.POINTS.ge(SIGN_CARD_COST)));
        if (rows == 0) {
            throw new BusinessException("积分不足，需要" + SIGN_CARD_COST + "积分");
        }

        Users updatedUser = usersMapper.selectOneById(userId);
        int newPoints = updatedUser.getPoints() != null ? updatedUser.getPoints() : 0;
        int newSignCards = updatedUser.getSignCards() != null ? updatedUser.getSignCards() : 0;

        PointsRecord pointsRecord = PointsRecord.builder()
                .userId(userId)
                .changeAmount(-SIGN_CARD_COST)
                .balanceAfter(newPoints)
                .type(PointsType.EXCHANGE_SIGN_CARD)
                .description("兑换补签卡")
                .createdAt(LocalDateTime.now())
                .build();
        pointsRecordMapper.insert(pointsRecord);

        return ExchangeSignCardResponse.builder()
                .success(true)
                .message("兑换成功")
                .costPoints(SIGN_CARD_COST)
                .remainingPoints(newPoints)
                .signCards(newSignCards)
                .build();
    }

    @Override
    public Page<SignRecordVO> getSignRecords(Long userId, int pageNumber, int pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SignRecord::getUserId).eq(userId)
                .orderBy(SignRecord::getSignDate, false);

        Page<SignRecord> page = signRecordMapper.paginate(
                Page.of(pageNumber, pageSize), queryWrapper);

        Page<SignRecordVO> voPage = new Page<>();
        voPage.setPageNumber(page.getPageNumber());
        voPage.setPageSize(page.getPageSize());
        voPage.setTotalRow(page.getTotalRow());

        List<SignRecordVO> voList = new ArrayList<>();
        for (SignRecord record : page.getRecords()) {
            SignRecordVO vo = SignRecordVO.builder()
                    .id(record.getId())
                    .signDate(record.getSignDate())
                    .expGained(record.getExpGained())
                    .pointsGained(record.getPointsGained())
                    .continuousDays(record.getContinuousDays())
                    .isRepair(record.getIsRepair())
                    .build();
            voList.add(vo);
        }
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public List<LocalDate> getSignCalendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SignRecord::getSignDate)
                .where(SignRecord::getUserId).eq(userId)
                .and(SignRecord::getSignDate).ge(start)
                .and(SignRecord::getSignDate).le(end)
                .orderBy(SignRecord::getSignDate, true);

        List<SignRecord> records = signRecordMapper.selectListByQuery(queryWrapper);

        List<LocalDate> dates = new ArrayList<>();
        for (SignRecord record : records) {
            dates.add(record.getSignDate());
        }

        return dates;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, Integer points, String type, Long relatedId, String description) {
        Users user = usersMapper.selectOneById(userId);
        if (user == null) {
            return;
        }

        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;

        int newPoints = currentPoints + points;

        Users updateUser = UpdateEntity.of(Users.class, userId);
        updateUser.setPoints(newPoints);
        usersMapper.update(updateUser);

        PointsRecord pointsRecord = PointsRecord.builder()
                .userId(userId)
                .changeAmount(points)
                .balanceAfter(newPoints)
                .type(PointsType.valueOf(type))
                .relatedId(relatedId)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        pointsRecordMapper.insert(pointsRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long userId, Integer points, String type, Long relatedId, String description) {
        Users user = usersMapper.selectOneById(userId);
        if (user == null) {
            return;
        }

        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        int newPoints = Math.max(0, currentPoints - points);

        Users updateUser = UpdateEntity.of(Users.class, userId);
        updateUser.setPoints(newPoints);
        usersMapper.update(updateUser);

        PointsRecord pointsRecord = PointsRecord.builder()
                .userId(userId)
                .changeAmount(-points)
                .balanceAfter(newPoints)
                .type(PointsType.valueOf(type))
                .relatedId(relatedId)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        pointsRecordMapper.insert(pointsRecord);
    }

    private Integer calculateContinuousDays(Long userId, LocalDate today) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SignRecord::getUserId).eq(userId)
                .and(SignRecord::getSignDate).eq(today.minusDays(1));
        SignRecord yesterdayRecord = signRecordMapper.selectOneByQuery(queryWrapper);

        if (yesterdayRecord != null) {
            return yesterdayRecord.getContinuousDays() + 1;
        }

        return 1;
    }

    private Integer calculateRepairContinuousDays(Long userId, LocalDate signDate) {
        // Use a range query to find all sign records from the earliest possible date up to signDate
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SignRecord::getUserId).eq(userId)
                .and(SignRecord::getSignDate).le(signDate)
                .orderBy(SignRecord::getSignDate, false);
        List<SignRecord> records = signRecordMapper.selectListByQuery(queryWrapper);

        int continuousDays = 0;
        LocalDate expectedDate = signDate;
        for (SignRecord record : records) {
            if (record.getSignDate().equals(expectedDate)) {
                continuousDays++;
                expectedDate = expectedDate.minusDays(1);
            } else if (record.getSignDate().isBefore(expectedDate)) {
                break;
            }
        }

        return continuousDays;
    }

    private void recalculateContinuousDaysAfterRepair(Long userId, LocalDate repairedDate) {
        LocalDate today = LocalDate.now();
        LocalDate checkDate = repairedDate.plusDays(1);

        while (!checkDate.isAfter(today)) {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .where(SignRecord::getUserId).eq(userId)
                    .and(SignRecord::getSignDate).eq(checkDate);
            SignRecord record = signRecordMapper.selectOneByQuery(queryWrapper);

            if (record != null) {
                int newContinuousDays = calculateRepairContinuousDays(userId, checkDate);

                SignRecord updateRecord = UpdateEntity.of(SignRecord.class, record.getId());
        updateRecord.setContinuousDays(newContinuousDays);
        signRecordMapper.update(updateRecord);
            }

            checkDate = checkDate.plusDays(1);
        }
    }

    private Integer calculateUserContinuousDays(Long userId, LocalDate today) {
        LocalDate lastSignDate = calculateUserLastSignDate(userId);
        if (lastSignDate == null) {
            return 0;
        }

        // Use a range query instead of N+1 daily queries
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SignRecord::getUserId).eq(userId)
                .and(SignRecord::getSignDate).le(lastSignDate)
                .orderBy(SignRecord::getSignDate, false);
        List<SignRecord> records = signRecordMapper.selectListByQuery(queryWrapper);

        int continuousDays = 0;
        LocalDate expectedDate = lastSignDate;
        for (SignRecord record : records) {
            if (record.getSignDate().equals(expectedDate)) {
                continuousDays++;
                expectedDate = expectedDate.minusDays(1);
            } else if (record.getSignDate().isBefore(expectedDate)) {
                break;
            }
        }

        return continuousDays;
    }

    private LocalDate calculateUserLastSignDate(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SignRecord::getUserId).eq(userId)
                .orderBy(SignRecord::getSignDate, false)
                .limit(1);
        SignRecord lastRecord = signRecordMapper.selectOneByQuery(queryWrapper);

        return lastRecord != null ? lastRecord.getSignDate() : null;
    }

    private int calculateExpReward(Integer continuousDays) {
        int exp = BASE_EXP;

        if (continuousDays >= 100) {
            exp += 50;
        } else if (continuousDays >= 60) {
            exp += 30;
        } else if (continuousDays >= 30) {
            exp += 20;
        } else if (continuousDays >= 15) {
            exp += 10;
        } else if (continuousDays >= 7) {
            exp += 5;
        } else if (continuousDays >= 3) {
            exp += 2;
        }

        return exp;
    }

    private int calculatePointsReward(Integer continuousDays) {
        int points = BASE_POINTS;

        if (continuousDays >= 100) {
            points += 25;
        } else if (continuousDays >= 60) {
            points += 15;
        } else if (continuousDays >= 30) {
            points += 10;
        } else if (continuousDays >= 15) {
            points += 5;
        } else if (continuousDays >= 7) {
            points += 3;
        } else if (continuousDays >= 3) {
            points += 1;
        }

        return points;
    }
}
