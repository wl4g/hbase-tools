-- ------------------------------------------------
-- ------------------------------------------------
-- 重要字段已注释，其他未注释字段与本演示无关.
-- ------------------------------------------------
-- ------------------------------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS `ed_customer`;
CREATE TABLE `ed_customer` (
  `customerId` varchar(20) NOT NULL,
  `industryCode` varchar(10) DEFAULT NULL COMMENT '',
  `comboId` varchar(4) DEFAULT NULL COMMENT '',
  `networkId` varchar(20) DEFAULT NULL COMMENT '',
  `ipId` int(20) DEFAULT NULL COMMENT '',
  `customerName` varchar(50) DEFAULT NULL COMMENT '',
  `customerAddr` varchar(64) DEFAULT NULL COMMENT '',
  `areaId` int(11) DEFAULT NULL COMMENT '',
  `addrCode` varchar(100) DEFAULT NULL COMMENT '',
  `isEnabled` int(11) DEFAULT NULL COMMENT '',
  `createTime` datetime DEFAULT NULL COMMENT '',
  `customerX` varchar(20) DEFAULT NULL COMMENT '',
  `customerY` varchar(20) DEFAULT NULL COMMENT '',
  `contactPerson` varchar(20) DEFAULT NULL COMMENT '',
  `contactPhone` varchar(20) DEFAULT NULL COMMENT '',
  `serviceContent` text COMMENT '',
  `shortName` varchar(32) DEFAULT NULL COMMENT '',
  `customerLogo` varchar(100) DEFAULT NULL COMMENT '',
  `operatePerson` varchar(20) DEFAULT NULL COMMENT '',
  `operatePhone` varchar(20) DEFAULT NULL COMMENT '',
  `operatePosition` varchar(20) DEFAULT '' COMMENT '',
  `customerManager` varchar(20) DEFAULT NULL COMMENT '',
  `customerManagerPhone` varchar(20) DEFAULT NULL COMMENT '',
  `serviceBeginDate` datetime DEFAULT NULL COMMENT '',
  `sort` bigint(20) DEFAULT NULL COMMENT '',
  `cid` varchar(64) NOT NULL,
  `isRegister` int(11) DEFAULT '0' COMMENT '',
  `isApprove` int(11) DEFAULT '-1' COMMENT '',
  `businessLicense` varchar(100) DEFAULT NULL COMMENT '',
  `language` varchar(255) DEFAULT 'cn' COMMENT '',
  `notifySwitch` varchar(255) DEFAULT '0' COMMENT '',
  `notifyIntervalTime` bigint(20) DEFAULT '600000',
  `notifyIntervalCount` int(2) DEFAULT '10',
  `isEmailPush` int(2) DEFAULT '0' COMMENT '',
  `emails` varchar(8000) DEFAULT NULL COMMENT '',
  `isPhonePush` int(2) DEFAULT '0' COMMENT '',
  `phones` varchar(8000) DEFAULT NULL COMMENT '',
  `haveRole` varchar(64) DEFAULT '' COMMENT '',
  `customerAcreage` double(16,2) DEFAULT NULL,
  `brief` text COMMENT '',
  `effIndexPic` varchar(50) DEFAULT NULL COMMENT '',
  `peopleNum` int(11) DEFAULT '0' COMMENT '',
  PRIMARY KEY (`customerId`),
  KEY `areaId` (`areaId`),
  KEY `customer_id` (`customerId`) USING BTREE,
  CONSTRAINT `ed_customer_ibfk_1` FOREIGN KEY (`areaId`) REFERENCES `ed_area` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='客户信息表';

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `safecloud`.`ed_customer` (`customerId`, `industryCode`, `comboId`, `networkId`, `ipId`, `customerName`, `customerAddr`, `areaId`, `addrCode`, `isEnabled`, `createTime`, `customerX`, `customerY`, `contactPerson`, `contactPhone`, `serviceContent`, `shortName`, `customerLogo`, `operatePerson`, `operatePhone`, `operatePosition`, `customerManager`, `customerManagerPhone`, `serviceBeginDate`, `sort`, `cid`, `isRegister`, `isApprove`, `businessLicense`, `language`, `notifySwitch`, `notifyIntervalTime`, `notifyIntervalCount`, `isEmailPush`, `emails`, `isPhonePush`, `phones`, `haveRole`, `customerAcreage`, `brief`, `effIndexPic`, `peopleNum`) VALUES ('4400429811581952', '102', '100', '101', NULL, '佛山市xxx印染有限公司', '广东省佛山市xxx', 440600, '{\"440000\",\"440600\",\"440605\"}', 0, '2019-05-09 23:31:36', '112.921127', '22.974474', '', '', '', 'xxx印染', '4946588186739712', '', '', '', '', '', '2019-05-09 00:00:00', 1624947280887, 'YQDYR', 0, -1, NULL, '', '0', 600000, 10, 0, NULL, 0, NULL, '4400420222518272', 18000.00, '', '', 0);

-- ------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS `ed_equiptemplate`;
CREATE TABLE `ed_equiptemplate` (
  `templateId` varchar(20) NOT NULL,
  `userId` varchar(20) DEFAULT NULL COMMENT '',
  `templateName` varchar(20) DEFAULT NULL COMMENT '',
  `templateType` int(11) DEFAULT NULL COMMENT '',
  `decription` varchar(255) DEFAULT NULL COMMENT '',
  `createTime` datetime DEFAULT NULL COMMENT '',
  `status` int(11) DEFAULT NULL COMMENT '',
  `IsCommonlyUse` int(11) DEFAULT NULL COMMENT '',
  `templateValue` varchar(20) DEFAULT NULL COMMENT '',
  `order` int(2) DEFAULT NULL COMMENT '',
  `level` int(11) DEFAULT NULL COMMENT '',
  `parentId` varchar(20) DEFAULT NULL COMMENT '',
  `templateMark` varchar(20) DEFAULT NULL COMMENT '',
  `scenesType` int(11) DEFAULT NULL COMMENT '',
  `icon` varchar(100) DEFAULT NULL,
  `icon2` varchar(50) DEFAULT NULL,
  `unit` varchar(50) DEFAULT NULL,
  `enTemplateName` varchar(255) DEFAULT NULL COMMENT '',
  `dev_sub_type` char(1) DEFAULT '0' COMMENT '',
  PRIMARY KEY (`templateId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='设备型号表 (配置模版)';

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `safecloud`.`ed_equiptemplate` (`templateId`, `userId`, `templateName`, `templateType`, `decription`, `createTime`, `status`, `IsCommonlyUse`, `templateValue`, `order`, `level`, `parentId`, `templateMark`, `scenesType`, `icon`, `icon2`, `unit`, `enTemplateName`, `dev_sub_type`) VALUES ('3703427920118784', '0002', '珠海xxx三相电表', 2, '', '2018-01-02 14:24:36', 0, 1, '', NULL, 2, '10000006', '134', 1, NULL, NULL, NULL, NULL, '0');

-- ------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS `ed_equipmentinfo`;
CREATE TABLE `ed_equipmentinfo` (
  `equipmentId` varchar(20) NOT NULL,
  `userId` varchar(20) DEFAULT NULL COMMENT '',
  `templateId` varchar(20) DEFAULT NULL COMMENT '所属设备型号, 对应 ed_equiptemplate.templateId',
  `customerId` varchar(20) DEFAULT NULL COMMENT '所属客户端, 对应 ed_customer.customerId',
  `departmentId` varchar(20) DEFAULT NULL COMMENT '',
  `equipmentName` varchar(100) DEFAULT NULL COMMENT '',
  `equaddress` varchar(255) DEFAULT NULL,
  `parkId` varchar(255) DEFAULT NULL COMMENT '',
  `attachEquipmentId` varchar(20) DEFAULT NULL COMMENT '',
  `switchingroomId` varchar(20) DEFAULT NULL COMMENT '',
  `maintenanceCycle` int(11) DEFAULT NULL COMMENT '',
  `parentEquipId` varchar(20) DEFAULT NULL COMMENT '',
  `comEquipId` varchar(20) DEFAULT NULL COMMENT '',
  `distrCabId` varchar(20) DEFAULT NULL COMMENT '',
  `addrXY` varchar(20) DEFAULT NULL COMMENT '',
  `equipmentType` int(11) DEFAULT NULL COMMENT '',
  `addrIP` varchar(20) DEFAULT NULL COMMENT '硬件采集设备主编号',
  `status` int(11) DEFAULT NULL COMMENT '',
  `addrIPOrder` int(11) DEFAULT NULL COMMENT '硬件采集设备次编号',
  `realSensorAddr` varchar(255) DEFAULT NULL COMMENT '',
  `unit` int(4) DEFAULT NULL COMMENT '',
  `display` int(11) DEFAULT NULL COMMENT '',
  `createTime` datetime DEFAULT NULL COMMENT '',
  `sort` bigint(20) DEFAULT NULL COMMENT '',
  `ptRate` varchar(20) DEFAULT NULL COMMENT '',
  `ctRate` varchar(20) DEFAULT NULL COMMENT '',
  `workingStatus` int(11) DEFAULT NULL COMMENT '',
  `enEquipmentName` varchar(255) DEFAULT NULL COMMENT '',
  `baseline` varchar(255) DEFAULT NULL COMMENT '',
  `point_x` char(15) DEFAULT '',
  `point_y` char(15) DEFAULT '',
  `productKey` varchar(64) DEFAULT NULL COMMENT '',
  `accessKey` varchar(16) DEFAULT NULL COMMENT '',
  `event_notify_status` varchar(2) DEFAULT '0' COMMENT '',
  `show_sort` varchar(255) DEFAULT NULL COMMENT '',
  PRIMARY KEY (`equipmentId`),
  KEY `NewIndex1` (`addrIP`,`status`,`addrIPOrder`),
  KEY `NewIndex2` (`customerId`),
  KEY `NewIndex3` (`templateId`),
  KEY `NewIndex4` (`attachEquipmentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='设备信息表';

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `safecloud`.`ed_equipmentinfo` (`equipmentId`, `userId`, `templateId`, `customerId`, `departmentId`, `equipmentName`, `equaddress`, `parkId`, `attachEquipmentId`, `switchingroomId`, `maintenanceCycle`, `parentEquipId`, `comEquipId`, `distrCabId`, `addrXY`, `equipmentType`, `addrIP`, `status`, `addrIPOrder`, `realSensorAddr`, `unit`, `display`, `createTime`, `sort`, `ptRate`, `ctRate`, `workingStatus`, `enEquipmentName`, `baseline`, `point_x`, `point_y`, `productKey`, `accessKey`, `event_notify_status`, `show_sort`) VALUES ('4400458386113536', '0003', '3703427920118784', '4400429811581952', '', '1#进线总开关电表', NULL, NULL, '4400442833503232', '', 0, NULL, NULL, '', '112.921127,22.974474', 2, '11111277', 0, 1, '1', 0, 3, '2019-05-10 00:00:40', 1557823935809, '1', '600', 1, NULL, '380', '112.921127', '22.974474', 'R6DdshYAdXxCpmgS', NULL, '1', '001');
