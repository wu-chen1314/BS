START TRANSACTION;

UPDATE ich_project SET features = '以西皮、二黄为核心声腔，行当完整，唱念做打程式成熟，脸谱与服饰辨识度高。' WHERE id = 1 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '水磨腔婉转细腻，曲词典雅，表演身段精致，兼具文学性、音乐性与舞台美感。' WHERE id = 2 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '梆黄体系与岭南方言结合，唱腔明快，武打与做表灵活，兼收南北戏曲元素。' WHERE id = 3 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '以高腔和帮腔见长，变脸、吐火等绝技极具观赏性，善于快速呈现人物情绪变化。' WHERE id = 4 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '板式变化丰富，唱腔高亢激越，锣鼓节奏强烈，舞台风格粗犷豪放。' WHERE id = 5 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '讲究吟、猱、绰、注等指法变化，音色深沉悠远，强调文人审美与意境表达。' WHERE id = 6 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '以吹打乐为主体，曲牌体系完整，保留唐宋音乐遗韵，节奏层次厚重庄严。' WHERE id = 7 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '旋律古朴舒缓，洞箫、琵琶、三弦与拍板配合紧密，保存大量中古音乐特征。' WHERE id = 8 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '针法细密灵动，擅长双面绣与细线分丝，画面层次丰富、色彩清雅。' WHERE id = 9 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '选料、蒸煮、漂白、抄纸等工序严谨，成纸柔韧耐久，润墨性和表现力突出。' WHERE id = 10 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '木机妆花织造复杂，图案华丽，常用金银线，织物层次丰厚且具有皇家气韵。' WHERE id = 11 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '拉坯、修坯、施釉、烧制环环相扣，胎釉结合精妙，体现瓷都传统工艺体系。' WHERE id = 12 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '以经络学说为基础，针法与灸法并用，强调辨证施治和整体调理。' WHERE id = 13 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '以道地药材、严谨炮制和传统店训为核心，体现中医药工艺与经营文化传承。' WHERE id = 14 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '包含赛龙舟、包粽子、挂艾草、佩香囊等习俗，兼具纪念、祈福与时令文化内涵。' WHERE id = 15 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '围绕祭典仪程、乐舞和海洋信俗展开，体现滨海社区共同体与航海祈安文化。' WHERE id = 16 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '涵盖唐卡、壁画、堆绣等门类，色彩浓烈，宗教题材鲜明，工艺传承师徒化明显。' WHERE id = 17 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '木版套印结合手工彩绘，构图喜庆饱满，人物与吉祥题材浓厚，装饰性强。' WHERE id = 18 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '强调以静制动、刚柔并济和呼吸协调，兼具健身、防身与哲学修习价值。' WHERE id = 19 AND (features IS NULL OR features = '');
UPDATE ich_project SET features = '以说唱和口头传承为主，篇幅宏大，人物谱系丰富，兼具史诗叙事与民族记忆功能。' WHERE id = 20 AND (features IS NULL OR features = '');

INSERT INTO ich_media (media_type, title, url, created_at)
SELECT 'image', CONCAT(p.name, ' 封面图'), p.cover_url, NOW()
FROM ich_project p
LEFT JOIN ich_media m ON m.url = p.cover_url
WHERE p.cover_url IS NOT NULL AND p.cover_url <> '' AND m.id IS NULL;

INSERT INTO ich_media (media_type, title, url, created_at)
SELECT 'video', CONCAT(p.name, ' 宣传视频'), p.video_url, NOW()
FROM ich_project p
LEFT JOIN ich_media m ON m.url = p.video_url
WHERE p.video_url IS NOT NULL AND p.video_url <> '' AND m.id IS NULL;

INSERT INTO ich_project_media (project_id, media_id, sort_no)
SELECT p.id, m.id, 1
FROM ich_project p
JOIN ich_media m ON m.url = p.cover_url AND m.media_type = 'image'
LEFT JOIN ich_project_media pm ON pm.project_id = p.id AND pm.media_id = m.id
WHERE p.cover_url IS NOT NULL AND p.cover_url <> '' AND pm.project_id IS NULL;

INSERT INTO ich_project_media (project_id, media_id, sort_no)
SELECT p.id, m.id, 2
FROM ich_project p
JOIN ich_media m ON m.url = p.video_url AND m.media_type = 'video'
LEFT JOIN ich_project_media pm ON pm.project_id = p.id AND pm.media_id = m.id
WHERE p.video_url IS NOT NULL AND p.video_url <> '' AND pm.project_id IS NULL;

COMMIT;