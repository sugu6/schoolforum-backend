-- 通用ID重排序脚本
-- 功能：直接对数据库中所有表的id进行重新排序（从1开始，步长为1）
-- 注意：此脚本会修改数据，执行前请务必备份数据库！

-- 暂时禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 对用户表重排序
SET @row_num = 0;
UPDATE `users` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `users` AUTO_INCREMENT = 1;

-- 对帖子表重排序
SET @row_num = 0;
UPDATE `posts` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `posts` AUTO_INCREMENT = 1;

-- 对评论表重排序
SET @row_num = 0;
UPDATE `comments` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `comments` AUTO_INCREMENT = 1;

-- 对公告表重排序
SET @row_num = 0;
UPDATE `announcements` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `announcements` AUTO_INCREMENT = 1;

-- 对标签表重排序
SET @row_num = 0;
UPDATE `tags` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `tags` AUTO_INCREMENT = 1;

-- 对分类表重排序
SET @row_num = 0;
UPDATE `categories` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `categories` AUTO_INCREMENT = 1;

-- 对关注表重排序
SET @row_num = 0;
UPDATE `follows` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `follows` AUTO_INCREMENT = 1;

-- 对收藏表重排序
SET @row_num = 0;
UPDATE `favorites` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `favorites` AUTO_INCREMENT = 1;

-- 对通知表重排序
SET @row_num = 0;
UPDATE `notifications` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `notifications` AUTO_INCREMENT = 1;

-- 对私信表重排序
SET @row_num = 0;
UPDATE `private_messages` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `private_messages` AUTO_INCREMENT = 1;

-- 对会话表重排序
SET @row_num = 0;
UPDATE `conversations` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `conversations` AUTO_INCREMENT = 1;

-- 对签到记录表重排序
SET @row_num = 0;
UPDATE `sign_records` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `sign_records` AUTO_INCREMENT = 1;

-- 对积分记录表重排序
SET @row_num = 0;
UPDATE `points_records` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `points_records` AUTO_INCREMENT = 1;

-- 对账号删除申请表重排序
SET @row_num = 0;
UPDATE `account_deletion_requests` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `account_deletion_requests` AUTO_INCREMENT = 1;

-- 对帖子标签关联表重排序
SET @row_num = 0;
UPDATE `post_tags` SET `id` = (@row_num := @row_num + 1) ORDER BY `id` ASC;
ALTER TABLE `post_tags` AUTO_INCREMENT = 1;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 执行完成后查看结果
SELECT 'ID重排序完成' AS result;