-- ============================================================
-- 东软智慧云脑诊疗平台 — 完整数据库定义 + 初始数据
-- 数据库名：cloud_brain_medical
-- 字符集：utf8mb4
-- 表数量：16 张
--
-- 用法（Navicat / MySQL CLI）：
--   1. 连接 MySQL 8.0+
--   2. 执行本脚本（会创建库、建表、插入演示数据）
--
-- 演示账号（BCrypt 加密，注意：不是 MySQL 连接密码 123456）：
--   admin       / admin123    角色 ADMIN
--   doctor1     / doctor123   角色 DOCTOR（张医生）
--   doctor2     / doctor123   角色 DOCTOR（李医生）
--   patient1    / patient123  角色 PATIENT
--   inspector1  / doctor123   角色 INSPECTOR
--   pharmacist1 / doctor123   角色 PHARMACIST
--
-- 枚举约定（与后端 Java enum 一致，存 VARCHAR/INT）：
--   user.role              PATIENT | DOCTOR | PHARMACIST | INSPECTOR | ADMIN
--   user.status            1=正常 0=禁用
--   patient.gender         1=男 2=女
--   schedule.time_slot     MORNING | AFTERNOON | EVENING
--   registration.status    PENDING | IN_PROGRESS | COMPLETED | CANCELLED
--   medical_record.status  DRAFT | CONFIRMED
--   prescription.status    PENDING_DISPENSE | DISPENSED
--   exam_order.status      PENDING | IN_PROGRESS | DONE
--   exam_order.order_type  LAB | EXAM
--   medical_equipment.status ACTIVE | DISABLED | MAINTENANCE | FAULT
--   prescription.risk_level LOW | MEDIUM | HIGH
--   lab_item.status / exam_item.status  ENABLED | DISABLED
-- ============================================================

CREATE DATABASE IF NOT EXISTS cloud_brain_medical
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE cloud_brain_medical;

-- ============================================================
-- 一、基础组织与用户（4 张）
-- ============================================================

-- 1. 科室
CREATE TABLE IF NOT EXISTS department (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name         VARCHAR(64)  NOT NULL COMMENT '科室名称',
    code         VARCHAR(16)           COMMENT '科室编码',
    description  TEXT                  COMMENT '科室简介',
    location     VARCHAR(64)           COMMENT '所在位置',
    sort_order   INT                   COMMENT '排序号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室';

-- 2. 系统用户（所有角色共用，patient/doctor 通过 id 关联扩展表）
CREATE TABLE IF NOT EXISTS `user` (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username      VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录名',
    password_hash VARCHAR(128) NOT NULL        COMMENT 'BCrypt 密码哈希',
    role          VARCHAR(20)  NOT NULL        COMMENT '角色：PATIENT/DOCTOR/PHARMACIST/INSPECTOR/ADMIN',
    real_name     VARCHAR(32)                  COMMENT '真实姓名',
    phone         VARCHAR(20)  UNIQUE          COMMENT '手机号',
    avatar_url    VARCHAR(256)                 COMMENT '头像 URL',
    status        INT          NOT NULL DEFAULT 1 COMMENT '1=正常 0=禁用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- 3. 患者扩展信息（id 与 user.id 一一对应）
CREATE TABLE IF NOT EXISTS patient (
    id                BIGINT PRIMARY KEY COMMENT '同 user.id',
    id_card           VARCHAR(18) COMMENT '身份证号',
    birth_date        DATE        COMMENT '出生日期',
    gender            INT         COMMENT '1=男 2=女',
    blood_type        VARCHAR(4)  COMMENT '血型',
    allergy_history   TEXT        COMMENT '过敏史',
    medical_history   TEXT        COMMENT '既往病史',
    emergency_contact VARCHAR(32) COMMENT '紧急联系人',
    emergency_phone   VARCHAR(20) COMMENT '紧急联系电话',
    CONSTRAINT fk_patient_user FOREIGN KEY (id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者信息';

-- 4. 医生扩展信息（id 与 user.id 一一对应）
CREATE TABLE IF NOT EXISTS doctor (
    id               BIGINT PRIMARY KEY COMMENT '同 user.id',
    department_id    BIGINT         COMMENT '所属科室',
    title            VARCHAR(32)    COMMENT '职称',
    specialty        VARCHAR(64)    COMMENT '专长',
    registration_fee DECIMAL(8,2)   COMMENT '挂号费（元）',
    introduction     TEXT           COMMENT '个人简介',
    is_available     INT DEFAULT 1  COMMENT '1=可接诊 0=停诊',
    CONSTRAINT fk_doctor_user FOREIGN KEY (id) REFERENCES `user`(id),
    CONSTRAINT fk_doctor_dept FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生信息';

-- ============================================================
-- 二、排班（1 张）
-- ============================================================

-- 5. 医生排班
CREATE TABLE IF NOT EXISTS schedule (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    doctor_id        BIGINT      NOT NULL COMMENT '医生 user.id',
    work_date        DATE        NOT NULL COMMENT '出诊日期',
    time_slot        VARCHAR(20) NOT NULL COMMENT '时段：MORNING/AFTERNOON/EVENING',
    max_patients     INT         NOT NULL COMMENT '号源上限',
    registered_count INT         NOT NULL DEFAULT 0 COMMENT '已挂号数',
    is_ai_generated  INT         DEFAULT 0 COMMENT '是否 AI 生成排班',
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生排班';

-- ============================================================
-- 三、就诊流程（1 张）
-- ============================================================

-- 6. 挂号/叫号
CREATE TABLE IF NOT EXISTS registration (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    reg_no         VARCHAR(32) NOT NULL UNIQUE COMMENT '挂号单号',
    patient_id     BIGINT      NOT NULL COMMENT '患者 user.id',
    doctor_id      BIGINT      NOT NULL COMMENT '医生 user.id',
    department_id  BIGINT      NOT NULL COMMENT '科室 id',
    schedule_id    BIGINT      NOT NULL COMMENT '排班 id',
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/CANCELLED',
    queue_no       INT                  COMMENT '排队序号',
    visit_type     INT                  COMMENT '就诊类型',
    chief_complaint TEXT                 COMMENT '主诉',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '挂号时间',
    paid_at        DATETIME             COMMENT '缴费时间',
    CONSTRAINT fk_reg_patient  FOREIGN KEY (patient_id)    REFERENCES `user`(id),
    CONSTRAINT fk_reg_doctor   FOREIGN KEY (doctor_id)     REFERENCES `user`(id),
    CONSTRAINT fk_reg_dept     FOREIGN KEY (department_id) REFERENCES department(id),
    CONSTRAINT fk_reg_schedule FOREIGN KEY (schedule_id)   REFERENCES schedule(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挂号记录';

-- ============================================================
-- 四、诊疗文书（4 张）
-- ============================================================

-- 8. 电子病历（与 registration 一对一）
CREATE TABLE IF NOT EXISTS medical_record (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    registration_id  BIGINT      NOT NULL UNIQUE COMMENT '挂号 id',
    patient_id       BIGINT      NOT NULL COMMENT '患者 user.id',
    doctor_id        BIGINT      NOT NULL COMMENT '医生 user.id',
    chief_complaint  TEXT                  COMMENT '主诉',
    present_illness  TEXT                  COMMENT '现病史',
    past_history     TEXT                  COMMENT '既往史',
    physical_exam    TEXT                  COMMENT '体格检查',
    diagnosis        TEXT                  COMMENT '诊断',
    treatment_plan   TEXT                  COMMENT '治疗方案',
    dialog_text      LONGTEXT              COMMENT '医患对话原文',
    ai_draft         LONGTEXT              COMMENT 'AI 生成草稿',
    is_ai_generated  INT         DEFAULT 0  COMMENT '是否 AI 生成',
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED',
    confirmed_at     DATETIME              COMMENT '确认时间',
    created_at       DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_mr_reg     FOREIGN KEY (registration_id) REFERENCES registration(id),
    CONSTRAINT fk_mr_patient FOREIGN KEY (patient_id)      REFERENCES `user`(id),
    CONSTRAINT fk_mr_doctor  FOREIGN KEY (doctor_id)       REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子病历';

-- 9. 处方主表
CREATE TABLE IF NOT EXISTS prescription (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    registration_id  BIGINT      NOT NULL COMMENT '挂号 id',
    doctor_id        BIGINT      NOT NULL COMMENT '开方医生 user.id',
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING_DISPENSE' COMMENT 'PENDING_DISPENSE/DISPENSED',
    ai_review_result TEXT                  COMMENT 'AI 审方结果',
    risk_level       VARCHAR(10)           COMMENT '风险等级：LOW/MEDIUM/HIGH',
    dispensed_by     BIGINT                COMMENT '发药药师 user.id',
    dispensed_at     DATETIME              COMMENT '发药时间',
    created_at       DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '开方时间',
    paid_at          DATETIME              COMMENT '缴费时间',
    CONSTRAINT fk_rx_reg        FOREIGN KEY (registration_id) REFERENCES registration(id),
    CONSTRAINT fk_rx_doctor     FOREIGN KEY (doctor_id)      REFERENCES `user`(id),
    CONSTRAINT fk_rx_pharmacist FOREIGN KEY (dispensed_by)   REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方';

-- 10. 处方明细
CREATE TABLE IF NOT EXISTS prescription_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    prescription_id BIGINT        NOT NULL COMMENT '处方 id',
    drug_id         BIGINT                 COMMENT '药品目录 id',
    drug_name       VARCHAR(128)  NOT NULL COMMENT '药品名称',
    drug_code       VARCHAR(32)            COMMENT '药品编码',
    dosage          DECIMAL(10,2)          COMMENT '单次剂量',
    unit            VARCHAR(16)            COMMENT '剂量单位',
    frequency       VARCHAR(32)            COMMENT '用药频次',
    duration_days   INT                    COMMENT '疗程（天）',
    total_amount    DECIMAL(10,2)          COMMENT '总量',
    CONSTRAINT fk_rx_item FOREIGN KEY (prescription_id) REFERENCES prescription(id),
    CONSTRAINT fk_rx_item_drug FOREIGN KEY (drug_id) REFERENCES drug(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方明细';

-- ============================================================
-- 检查检验项目目录（2 张，初始无数据，须在 exam_order 之前）
-- ============================================================

-- 11. 检验项目目录（无需设备，如血常规、尿常规）
CREATE TABLE IF NOT EXISTS lab_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    item_code       VARCHAR(32)  NOT NULL UNIQUE COMMENT '检验项目编码',
    item_name       VARCHAR(128) NOT NULL        COMMENT '检验项目名称',
    category        VARCHAR(64)  NOT NULL        COMMENT '检验类别',
    description     TEXT                  COMMENT '详细说明',
    department_id   BIGINT       NOT NULL COMMENT '所属科室（检验科）',
    fee             DECIMAL(10,2) NOT NULL COMMENT '检验费用（元）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED=启用 DISABLED=停用',
    CONSTRAINT fk_lab_item_dept FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验项目目录';

-- 12. 检查项目目录（需使用设备，如 CT、X 光）
CREATE TABLE IF NOT EXISTS exam_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    item_code       VARCHAR(32)  NOT NULL UNIQUE COMMENT '检查项目编码',
    item_name       VARCHAR(128) NOT NULL        COMMENT '检查项目名称',
    body_part       VARCHAR(64)           COMMENT '检查部位',
    description     TEXT                  COMMENT '详细说明',
    department_id   BIGINT       NOT NULL COMMENT '所属科室（检查科）',
    equipment_type  VARCHAR(64)  NOT NULL COMMENT '设备类型',
    fee             DECIMAL(10,2) NOT NULL COMMENT '检查费用（元）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED=启用 DISABLED=停用',
    CONSTRAINT fk_exam_item_dept FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查项目目录';

-- 13. 检查/检验申请单（业务流程，关联 registration 与项目目录）
CREATE TABLE IF NOT EXISTS exam_order (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    registration_id     BIGINT      NOT NULL COMMENT '挂号 id',
    order_type          VARCHAR(10) NOT NULL DEFAULT 'EXAM' COMMENT 'LAB=检验 EXAM=检查',
    lab_item_id         BIGINT               COMMENT '检验项目 id',
    exam_item_id        BIGINT               COMMENT '检查项目 id',
    exam_type           VARCHAR(32) NOT NULL COMMENT '项目名称（冗余展示）',
    body_part           VARCHAR(64)          COMMENT '检查部位/检验类别',
    clinical_indication TEXT                 COMMENT '临床指征',
    fee                 DECIMAL(10,2)        COMMENT '开立时费用（元）',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/DONE',
    ordered_by          BIGINT      NOT NULL COMMENT '开单医生 user.id',
    created_at          DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '开单时间',
    paid_at             DATETIME             COMMENT '缴费时间',
    CONSTRAINT fk_exam_order_reg       FOREIGN KEY (registration_id) REFERENCES registration(id),
    CONSTRAINT fk_exam_order_doctor    FOREIGN KEY (ordered_by)      REFERENCES `user`(id),
    CONSTRAINT fk_exam_order_lab_item  FOREIGN KEY (lab_item_id)     REFERENCES lab_item(id),
    CONSTRAINT fk_exam_order_exam_item FOREIGN KEY (exam_item_id)   REFERENCES exam_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查检验申请单';

-- 14. 检查/检验报告（与 exam_order 一对一）
CREATE TABLE IF NOT EXISTS exam_report (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_id        BIGINT   NOT NULL UNIQUE COMMENT '申请单 id',
    report_text     TEXT              COMMENT '报告正文',
    conclusion      TEXT              COMMENT '结论',
    image_urls      TEXT              COMMENT '影像 URL 列表',
    is_ai_generated INT      DEFAULT 0 COMMENT '是否 AI 生成',
    inspector_id    BIGINT            COMMENT '检查员 user.id',
    confirmed_at    DATETIME          COMMENT '确认时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_exam_report_order     FOREIGN KEY (order_id)       REFERENCES exam_order(id),
    CONSTRAINT fk_exam_report_inspector FOREIGN KEY (inspector_id)   REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查检验报告';

-- ============================================================
-- 五、药品与设备（3 张）
-- ============================================================

-- 15. 药品目录
CREATE TABLE IF NOT EXISTS drug (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    drug_code      VARCHAR(32)  NOT NULL UNIQUE COMMENT '药品编码',
    drug_name      VARCHAR(128) NOT NULL        COMMENT '药品名称',
    specification  VARCHAR(64)                  COMMENT '规格',
    unit           VARCHAR(16)                  COMMENT '销售单位',
    unit_price     DECIMAL(10,2)                COMMENT '单价（元）',
    manufacturer   VARCHAR(128)                 COMMENT '生产厂家',
    contraindication TEXT                     COMMENT '禁忌症',
    status         INT          NOT NULL DEFAULT 1 COMMENT '1=在售 0=下架',
    stock_quantity INT          NOT NULL DEFAULT 100 COMMENT '库存数量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品目录';

-- 16. 医疗设备
CREATE TABLE IF NOT EXISTS medical_equipment (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    equipment_code       VARCHAR(32)  NOT NULL UNIQUE COMMENT '设备编码',
    equipment_name       VARCHAR(128) NOT NULL        COMMENT '设备名称',
    category             VARCHAR(64)  NOT NULL        COMMENT '设备分类',
    department_id        BIGINT                       COMMENT '归属科室',
    model                VARCHAR(64)                  COMMENT '型号',
    manufacturer         VARCHAR(128)                 COMMENT '厂商',
    location             VARCHAR(128)                 COMMENT '存放位置',
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED/MAINTENANCE/FAULT',
    purchase_date        DATE                         COMMENT '购置日期',
    last_maintenance_date DATE                        COMMENT '上次维护日期',
    remark               TEXT                         COMMENT '备注',
    created_at           DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_equipment_dept FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医疗设备';

-- 17. 设备使用记录
CREATE TABLE IF NOT EXISTS equipment_usage_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    equipment_id  BIGINT      NOT NULL COMMENT '设备 id',
    usage_type    VARCHAR(32) NOT NULL COMMENT '使用类型',
    operator_name VARCHAR(64)          COMMENT '操作人姓名',
    patient_info  VARCHAR(128)         COMMENT '关联患者信息',
    start_time    DATETIME    NOT NULL COMMENT '开始时间',
    end_time      DATETIME             COMMENT '结束时间',
    remark        TEXT                 COMMENT '备注',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_usage_equipment FOREIGN KEY (equipment_id) REFERENCES medical_equipment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备使用记录';

-- ============================================================
-- 初始数据
-- ============================================================

-- ---------- 科室 ----------
INSERT INTO department (name, code, description, location, sort_order) VALUES
('心内科',   'CARDIO', '心血管内科',     '门诊楼 2F', 1),
('呼吸内科', 'RESP',   '呼吸与危重症',   '门诊楼 2F', 2),
('骨科',     'ORTHO',  '骨科与运动医学', '门诊楼 3F', 3),
('急诊科',   'ER',     '24小时急诊',     '急诊楼 1F', 4),
('检验科',   'LAB',    '临床检验',       '医技楼 3F', 5),
('检查科',   'RAD',    '医学影像检查',   '医技楼 1F', 6);

-- ---------- 用户（id 固定便于关联扩展表） ----------
-- admin / admin123
INSERT INTO `user` (id, username, password_hash, role, real_name, phone, status) VALUES
(1, 'admin',       '$2a$10$q/zw7tETCrFjtMLuld0luO6XM3xIJrDWEVp1VS1jIkIznPqdJm27a', 'ADMIN',      '系统管理员', '13800000001', 1),
(2, 'doctor1',     '$2a$10$eY0n7wshX7CGbr3IfEwj0eNWXR9rgdzn4KPoWjid0V79wIi9aohLG', 'DOCTOR',     '张医生',     '13800000002', 1),
(3, 'patient1',    '$2a$10$v7czR0DaO.MRXp7sC1DSk.YSYIP9AvM8hnEOvkvIfe.j1mey5v2VS', 'PATIENT',    '李患者',     '13800000003', 1),
(4, 'inspector1',  '$2a$10$eY0n7wshX7CGbr3IfEwj0eNWXR9rgdzn4KPoWjid0V79wIi9aohLG', 'INSPECTOR',  '王检查',     '13800000004', 1),
(5, 'pharmacist1', '$2a$10$eY0n7wshX7CGbr3IfEwj0eNWXR9rgdzn4KPoWjid0V79wIi9aohLG', 'PHARMACIST', '赵药师',     '13800000005', 1),
(6, 'doctor2',     '$2a$10$eY0n7wshX7CGbr3IfEwj0eNWXR9rgdzn4KPoWjid0V79wIi9aohLG', 'DOCTOR',     '李医生',     '13800000006', 1);

-- ---------- 医生 / 患者扩展 ----------
INSERT INTO doctor (id, department_id, title, specialty, registration_fee, introduction, is_available) VALUES
(2, 1, '主任医师', '冠心病、高血压', 50.00, '从事心内科临床工作20年', 1),
(6, 2, '副主任医师', '呼吸系统疾病', 40.00, '从事呼吸内科临床工作10年', 1);

INSERT INTO patient (id, gender, allergy_history) VALUES
(3, 1, '青霉素过敏');

-- ---------- 排班（当天上午/下午） ----------
INSERT INTO schedule (doctor_id, work_date, time_slot, max_patients, registered_count) VALUES
(2, CURDATE(), 'MORNING',   20, 0),
(2, CURDATE(), 'AFTERNOON', 20, 0),
(6, CURDATE(), 'MORNING',   20, 0),
(6, CURDATE(), 'AFTERNOON', 20, 0);

-- ---------- 药品目录 ----------
INSERT INTO drug (drug_code, drug_name, specification, unit, unit_price, manufacturer, status, stock_quantity) VALUES
('DRUG001', '阿司匹林肠溶片', '100mg*30片',  '盒', 15.50, '示例制药', 1, 100),
('DRUG002', '硝酸甘油片',     '0.5mg*100片', '瓶',  8.00, '示例制药', 1, 100),
('DRUG003', '阿莫西林胶囊',   '0.25g*24粒',  '盒', 12.00, '示例制药', 1, 100);

-- ---------- 医疗设备 ----------
INSERT INTO medical_equipment (equipment_code, equipment_name, category, department_id, model, manufacturer, location, status, purchase_date, last_maintenance_date) VALUES
('EQ-CT-001',  '64排螺旋CT',       '影像设备', 1, 'Revolution CT',   'GE医疗',      '影像科 1F', 'ACTIVE',      '2022-03-15', CURDATE()),
('EQ-XR-001',  '数字化X光机',      '影像设备', 2, 'DRX-Revolution',  'Carestream',  '放射科 2F', 'MAINTENANCE', '2021-06-20', '2026-05-01'),
('EQ-LAB-001', '全自动生化分析仪', '检验设备', 1, 'AU5800',          '贝克曼',      '检验科 3F', 'ACTIVE',      '2023-01-10', NULL),
('EQ-MON-001', '多参数监护仪',     '监护设备', 4, 'BeneVision N22',  '迈瑞',        '急诊科',    'FAULT',       '2020-08-05', NULL);

-- ============================================================
-- 以下表暂无初始数据，由业务流程或管理端维护时写入：
--   registration, medical_record,
--   prescription, prescription_item, exam_order, exam_report,
--   equipment_usage_record,
--   lab_item, exam_item
-- ============================================================
