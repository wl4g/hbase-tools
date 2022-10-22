package com.wl4g.tools.hbase.phoenix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pql")
public class PhoenixHbaseToolsProperties {
    private String saveOrUpdateAir;
    private String saveOrUpdateAmmeter;
    private String saveOrUpdateAtmos;
    private String saveOrUpdateElecFreq;
    private String saveOrUpdateElecLineV;
    private String saveOrUpdatePhaseI;
    private String saveOrUpdatePhaseV;
    private String saveOrUpdatePower;
    private String saveOrUpdatePowerFactor;
    private String saveOrUpdateHum;
    private String saveOrUpdateRay;
    private String saveOrUpdateSmoke;
    private String saveOrUpdateWater;
    private String saveOrUpdateWaterNjyc;
    private String selectDoorByPublicParamBean;
    // 查询
    private String selectHumByPublicParamBean;

    private String selectApparentTotal;

    private String selectTempByPublicParamBean;

    private String selectPM25ByPublicParamBean;

    private String selectCoByPublicParamBean;
    private String selectCo2ByPublicParamBean;
    private String selectHchoByPublicParamBean;
    private String selectTvocByPublicParamBean;
    private String selectLargestDemandAByPublicParamBean;
    private String selectLargestDemandRByPublicParamBean;
    private String selectLargestDemandTByPublicParamBean;

    private String selectPM10ByPublicParamBean;

    private String selectWaterByPublicParamBean;

    private String selectSmokeByPublicParamBean;

    private String selectRayByPublicParamBean;

    private String selectPressByPublicParamBean;

    private String selectRealtimeFlowByPublicParamBean;

    private String selectTotalFlowByPublicParamBean;

    private String selectFreqByPublicParamBean;

    private String selectPowerFactorByPublicParamBean;

    private String selectPowerFactorTotalByPublicParamBean;

    private String selectPowerFactorAByPublicParamBean;

    private String selectPowerFactorBByPublicParamBean;

    private String selectPowerFactorCByPublicParamBean;

    private String selectActiveByPublicParamBean;

    private String selectActiveAByPublicParamBean;

    private String selectAmmeter;

    private String selectActiveBByPublicParamBean;

    private String selectActiveCByPublicParamBean;

    private String selectActiveTotalByPublicParamBean;

    private String selectReactiveByPublicParamBean;

    private String selectReactiveAByPublicParamBean;

    private String selectReactiveBByPublicParamBean;

    private String selectReactiveCByPublicParamBean;

    private String selectReactiveTotalByPublicParamBean;

    private String selectApparentByPublicParamBean;

    private String selectApparentAByPublicParamBean;

    private String selectApparentBByPublicParamBean;

    private String selectApparentCByPublicParamBean;

    private String selectApparentTotalByPublicParamBean;

    private String machineCycle;

    private String selectLineVByPublicParamBean;

    private String selectLineVABByPublicParamBean;

    private String selectLineVBCByPublicParamBean;

    private String selectLineVCAByPublicParamBean;

    private String selectPhaseIByPublicParamBean;

    private String selectPhaseIAByPublicParamBean;

    private String selectPhaseIBByPublicParamBean;

    private String selectPhaseICByPublicParamBean;

    private String selectPhaseVByPublicParamBean;

    private String selectPhaseVAByPublicParamBean;

    private String selectPhaseVBByPublicParamBean;

    private String selectPhaseVCByPublicParamBean;

    private String selectActivePowerByPublicParamBean;

    private String selectReactivePowerByPublicParamBean;

    private String selectRateVByPublicParamBean;

    private String selectRateIByPublicParamBean;

    private String selectFreqRateByPublicParamBean;

    private String selectPhaseVRateByPublicParamBean;

    private String selectDataRage;

    private String selectRateAByPublicParamBean;

    private String selectRateBByPublicParamBean;

    private String selectRateCByPublicParamBean;

    private String selectDemandByPublicParamBean;

    private String selectActiveDemandByPublicParamBean;

    private String selectReactiveDemandByPublicParamBean;

    private String selectTotalDemandByPublicParamBean;

    private String selectHarmonicIByPublicParamBean;
    private String selectHarmonicIAByPublicParamBean;
    private String selectHarmonicIBByPublicParamBean;
    private String selectHarmonicICByPublicParamBean;

    private String selectHarmonicVByPublicParamBean;
    private String selectHarmonicVAByPublicParamBean;
    private String selectHarmonicVBByPublicParamBean;
    private String selectHarmonicVCByPublicParamBean;

    private String selectPhaseVRateASqlBean;
    private String selectPhaseVRateBSqlBean;
    private String selectPhaseVRateCSqlBean;

    private String selectRateISqlBean;
    private String selectRateVSqlBean;

    private String selectDataStatisticsGroupByCid;
    private String selectDataStatisticsGroupByTable;
    private String selectDataStatisticsGroupByBid;

    private String selectHistoryDynamicParam; // Dynamic parameter
                                              // select-history.
    private String selectMaxDynamicParam; // Dynamic parameter select
    // history.

    private String selectCommomHistoryDynamicParam;

    private String selectCommomMaxDynamicParam;

    private String selectChargingStateOnGenericByPublicParamBean;

    public String getSelectChargingStateOnGenericByPublicParamBean() {
        return selectChargingStateOnGenericByPublicParamBean;
    }

    public void setSelectChargingStateOnGenericByPublicParamBean(String selectChargingStateOnGenericByPublicParamBean) {
        this.selectChargingStateOnGenericByPublicParamBean = selectChargingStateOnGenericByPublicParamBean;
    }

    public String getSelectCommomMaxDynamicParam() {
        return selectCommomMaxDynamicParam;
    }

    public void setSelectCommomMaxDynamicParam(String selectCommomMaxDynamicParam) {
        this.selectCommomMaxDynamicParam = selectCommomMaxDynamicParam;
    }

    public String getSelectCommomHistoryDynamicParam() {
        return selectCommomHistoryDynamicParam;
    }

    public void setSelectCommomHistoryDynamicParam(String selectCommomHistoryDynamicParam) {
        this.selectCommomHistoryDynamicParam = selectCommomHistoryDynamicParam;
    }

    public String getSelectHistoryDynamicParam() {
        return selectHistoryDynamicParam;
    }

    public String getSelectRateISqlBean() {
        return selectRateISqlBean;
    }

    public void setSelectRateISqlBean(String selectRateISqlBean) {
        this.selectRateISqlBean = selectRateISqlBean;
    }

    public String getSelectRateVSqlBean() {
        return selectRateVSqlBean;
    }

    public void setSelectRateVSqlBean(String selectRateVSqlBean) {
        this.selectRateVSqlBean = selectRateVSqlBean;
    }

    public String getSelectPhaseVRateASqlBean() {
        return selectPhaseVRateASqlBean;
    }

    public void setSelectPhaseVRateASqlBean(String selectPhaseVRateASqlBean) {
        this.selectPhaseVRateASqlBean = selectPhaseVRateASqlBean;
    }

    public String getSelectPhaseVRateBSqlBean() {
        return selectPhaseVRateBSqlBean;
    }

    public void setSelectPhaseVRateBSqlBean(String selectPhaseVRateBSqlBean) {
        this.selectPhaseVRateBSqlBean = selectPhaseVRateBSqlBean;
    }

    public String getSelectPhaseVRateCSqlBean() {
        return selectPhaseVRateCSqlBean;
    }

    public void setSelectPhaseVRateCSqlBean(String selectPhaseVRateCSqlBean) {
        this.selectPhaseVRateCSqlBean = selectPhaseVRateCSqlBean;
    }

    public String getSelectMaxDynamicParam() {
        return selectMaxDynamicParam;
    }

    public void setSelectMaxDynamicParam(String selectMaxDynamicParam) {
        this.selectMaxDynamicParam = selectMaxDynamicParam;
    }

    public void setSelectHistoryDynamicParam(String selectHistoryPublicParam) {
        this.selectHistoryDynamicParam = selectHistoryPublicParam;
    }

    public String getSelectDataStatisticsGroupByBid() {
        return selectDataStatisticsGroupByBid;
    }

    public void setSelectDataStatisticsGroupByBid(String selectDataStatisticsGroupByBid) {
        this.selectDataStatisticsGroupByBid = selectDataStatisticsGroupByBid;
    }

    public String getSelectDataStatisticsGroupByCid() {
        return selectDataStatisticsGroupByCid;
    }

    public void setSelectDataStatisticsGroupByCid(String selectDataStatisticsGroupByCid) {
        this.selectDataStatisticsGroupByCid = selectDataStatisticsGroupByCid;
    }

    public String getSelectDataStatisticsGroupByTable() {
        return selectDataStatisticsGroupByTable;
    }

    public void setSelectDataStatisticsGroupByTable(String selectDataStatisticsGroupByTable) {
        this.selectDataStatisticsGroupByTable = selectDataStatisticsGroupByTable;
    }

    public String getSelectHarmonicVByPublicParamBean() {
        return selectHarmonicVByPublicParamBean;
    }

    public void setSelectHarmonicVByPublicParamBean(String selectHarmonicVByPublicParamBean) {
        this.selectHarmonicVByPublicParamBean = selectHarmonicVByPublicParamBean;
    }

    public String getSelectHarmonicVAByPublicParamBean() {
        return selectHarmonicVAByPublicParamBean;
    }

    public void setSelectHarmonicVAByPublicParamBean(String selectHarmonicVAByPublicParamBean) {
        this.selectHarmonicVAByPublicParamBean = selectHarmonicVAByPublicParamBean;
    }

    public String getSelectHarmonicVBByPublicParamBean() {
        return selectHarmonicVBByPublicParamBean;
    }

    public void setSelectHarmonicVBByPublicParamBean(String selectHarmonicVBByPublicParamBean) {
        this.selectHarmonicVBByPublicParamBean = selectHarmonicVBByPublicParamBean;
    }

    public String getSelectHarmonicVCByPublicParamBean() {
        return selectHarmonicVCByPublicParamBean;
    }

    public void setSelectHarmonicVCByPublicParamBean(String selectHarmonicVCByPublicParamBean) {
        this.selectHarmonicVCByPublicParamBean = selectHarmonicVCByPublicParamBean;
    }

    public String getSelectHarmonicIByPublicParamBean() {
        return selectHarmonicIByPublicParamBean;
    }

    public void setSelectHarmonicIByPublicParamBean(String selectHarmonicIByPublicParamBean) {
        this.selectHarmonicIByPublicParamBean = selectHarmonicIByPublicParamBean;
    }

    public String getSelectHarmonicIAByPublicParamBean() {
        return selectHarmonicIAByPublicParamBean;
    }

    public void setSelectHarmonicIAByPublicParamBean(String selectHarmonicIAByPublicParamBean) {
        this.selectHarmonicIAByPublicParamBean = selectHarmonicIAByPublicParamBean;
    }

    public String getSelectHarmonicIBByPublicParamBean() {
        return selectHarmonicIBByPublicParamBean;
    }

    public void setSelectHarmonicIBByPublicParamBean(String selectHarmonicIBByPublicParamBean) {
        this.selectHarmonicIBByPublicParamBean = selectHarmonicIBByPublicParamBean;
    }

    public String getSelectHarmonicICByPublicParamBean() {
        return selectHarmonicICByPublicParamBean;
    }

    public void setSelectHarmonicICByPublicParamBean(String selectHarmonicICByPublicParamBean) {
        this.selectHarmonicICByPublicParamBean = selectHarmonicICByPublicParamBean;
    }

    public String getSelectDemandByPublicParamBean() {
        return selectDemandByPublicParamBean;
    }

    public void setSelectDemandByPublicParamBean(String selectDemandByPublicParamBean) {
        this.selectDemandByPublicParamBean = selectDemandByPublicParamBean;
    }

    public String getSelectActiveDemandByPublicParamBean() {
        return selectActiveDemandByPublicParamBean;
    }

    public void setSelectActiveDemandByPublicParamBean(String selectActiveDemandByPublicParamBean) {
        this.selectActiveDemandByPublicParamBean = selectActiveDemandByPublicParamBean;
    }

    public String getSelectReactiveDemandByPublicParamBean() {
        return selectReactiveDemandByPublicParamBean;
    }

    public void setSelectReactiveDemandByPublicParamBean(String selectReactiveDemandByPublicParamBean) {
        this.selectReactiveDemandByPublicParamBean = selectReactiveDemandByPublicParamBean;
    }

    public String getSelectTotalDemandByPublicParamBean() {
        return selectTotalDemandByPublicParamBean;
    }

    public void setSelectTotalDemandByPublicParamBean(String selectTotalDemandByPublicParamBean) {
        this.selectTotalDemandByPublicParamBean = selectTotalDemandByPublicParamBean;
    }

    public String getSelectPhaseVRateByPublicParamBean() {
        return selectPhaseVRateByPublicParamBean;
    }

    public void setSelectPhaseVRateByPublicParamBean(String selectPhaseVRateByPublicParamBean) {
        this.selectPhaseVRateByPublicParamBean = selectPhaseVRateByPublicParamBean;
    }

    public String getSelectRateAByPublicParamBean() {
        return selectRateAByPublicParamBean;
    }

    public void setSelectRateAByPublicParamBean(String selectRateAByPublicParamBean) {
        this.selectRateAByPublicParamBean = selectRateAByPublicParamBean;
    }

    public String getSelectRateBByPublicParamBean() {
        return selectRateBByPublicParamBean;
    }

    public void setSelectRateBByPublicParamBean(String selectRateBByPublicParamBean) {
        this.selectRateBByPublicParamBean = selectRateBByPublicParamBean;
    }

    public String getSelectRateCByPublicParamBean() {
        return selectRateCByPublicParamBean;
    }

    public void setSelectRateCByPublicParamBean(String selectRateCByPublicParamBean) {
        this.selectRateCByPublicParamBean = selectRateCByPublicParamBean;
    }

    public String getSelectFreqRateByPublicParamBean() {
        return selectFreqRateByPublicParamBean;
    }

    public void setSelectFreqRateByPublicParamBean(String selectFreqRateByPublicParamBean) {
        this.selectFreqRateByPublicParamBean = selectFreqRateByPublicParamBean;
    }

    public String getSelectRateIByPublicParamBean() {
        return selectRateIByPublicParamBean;
    }

    public void setSelectRateIByPublicParamBean(String selectRateIByPublicParamBean) {
        this.selectRateIByPublicParamBean = selectRateIByPublicParamBean;
    }

    public String getSelectRateVByPublicParamBean() {
        return selectRateVByPublicParamBean;
    }

    public void setSelectRateVByPublicParamBean(String selectRateVByPublicParamBean) {
        this.selectRateVByPublicParamBean = selectRateVByPublicParamBean;
    }

    public String getSelectApparentAByPublicParamBean() {
        return selectApparentAByPublicParamBean;
    }

    public void setSelectApparentAByPublicParamBean(String selectApparentAByPublicParamBean) {
        this.selectApparentAByPublicParamBean = selectApparentAByPublicParamBean;
    }

    public String getSelectApparentBByPublicParamBean() {
        return selectApparentBByPublicParamBean;
    }

    public void setSelectApparentBByPublicParamBean(String selectApparentBByPublicParamBean) {
        this.selectApparentBByPublicParamBean = selectApparentBByPublicParamBean;
    }

    public String getSelectApparentCByPublicParamBean() {
        return selectApparentCByPublicParamBean;
    }

    public void setSelectApparentCByPublicParamBean(String selectApparentCByPublicParamBean) {
        this.selectApparentCByPublicParamBean = selectApparentCByPublicParamBean;
    }

    public String getSelectApparentTotalByPublicParamBean() {
        return selectApparentTotalByPublicParamBean;
    }

    public void setSelectApparentTotalByPublicParamBean(String selectApparentTotalByPublicParamBean) {
        this.selectApparentTotalByPublicParamBean = selectApparentTotalByPublicParamBean;
    }

    public String getSelectReactiveAByPublicParamBean() {
        return selectReactiveAByPublicParamBean;
    }

    public void setSelectReactiveAByPublicParamBean(String selectReactiveAByPublicParamBean) {
        this.selectReactiveAByPublicParamBean = selectReactiveAByPublicParamBean;
    }

    public String getSelectReactiveBByPublicParamBean() {
        return selectReactiveBByPublicParamBean;
    }

    public void setSelectReactiveBByPublicParamBean(String selectReactiveBByPublicParamBean) {
        this.selectReactiveBByPublicParamBean = selectReactiveBByPublicParamBean;
    }

    public String getSelectApparentTotal() {
        return selectApparentTotal;
    }

    public void setSelectApparentTotal(String selectApparentTotal) {
        this.selectApparentTotal = selectApparentTotal;
    }

    public String getSelectReactiveCByPublicParamBean() {
        return selectReactiveCByPublicParamBean;
    }

    public void setSelectReactiveCByPublicParamBean(String selectReactiveCByPublicParamBean) {
        this.selectReactiveCByPublicParamBean = selectReactiveCByPublicParamBean;
    }

    public String getSelectReactiveTotalByPublicParamBean() {
        return selectReactiveTotalByPublicParamBean;
    }

    public void setSelectReactiveTotalByPublicParamBean(String selectReactiveTotalByPublicParamBean) {
        this.selectReactiveTotalByPublicParamBean = selectReactiveTotalByPublicParamBean;
    }

    public String getSelectActiveAByPublicParamBean() {
        return selectActiveAByPublicParamBean;
    }

    public void setSelectActiveAByPublicParamBean(String selectActiveAByPublicParamBean) {
        this.selectActiveAByPublicParamBean = selectActiveAByPublicParamBean;
    }

    public String getSelectActiveBByPublicParamBean() {
        return selectActiveBByPublicParamBean;
    }

    public void setSelectActiveBByPublicParamBean(String selectActiveBByPublicParamBean) {
        this.selectActiveBByPublicParamBean = selectActiveBByPublicParamBean;
    }

    public String getSelectActiveCByPublicParamBean() {
        return selectActiveCByPublicParamBean;
    }

    public void setSelectActiveCByPublicParamBean(String selectActiveCByPublicParamBean) {
        this.selectActiveCByPublicParamBean = selectActiveCByPublicParamBean;
    }

    public String getSelectActiveTotalByPublicParamBean() {
        return selectActiveTotalByPublicParamBean;
    }

    public void setSelectActiveTotalByPublicParamBean(String selectActiveTotalByPublicParamBean) {
        this.selectActiveTotalByPublicParamBean = selectActiveTotalByPublicParamBean;
    }

    public String getSelectPhaseIAByPublicParamBean() {
        return selectPhaseIAByPublicParamBean;
    }

    public void setSelectPhaseIAByPublicParamBean(String selectPhaseIAByPublicParamBean) {
        this.selectPhaseIAByPublicParamBean = selectPhaseIAByPublicParamBean;
    }

    public String getSelectPhaseIBByPublicParamBean() {
        return selectPhaseIBByPublicParamBean;
    }

    public void setSelectPhaseIBByPublicParamBean(String selectPhaseIBByPublicParamBean) {
        this.selectPhaseIBByPublicParamBean = selectPhaseIBByPublicParamBean;
    }

    public String getSelectPhaseICByPublicParamBean() {
        return selectPhaseICByPublicParamBean;
    }

    public void setSelectPhaseICByPublicParamBean(String selectPhaseICByPublicParamBean) {
        this.selectPhaseICByPublicParamBean = selectPhaseICByPublicParamBean;
    }

    public String getSelectPhaseVAByPublicParamBean() {
        return selectPhaseVAByPublicParamBean;
    }

    public void setSelectPhaseVAByPublicParamBean(String selectPhaseVAByPublicParamBean) {
        this.selectPhaseVAByPublicParamBean = selectPhaseVAByPublicParamBean;
    }

    public String getSelectPhaseVBByPublicParamBean() {
        return selectPhaseVBByPublicParamBean;
    }

    public void setSelectPhaseVBByPublicParamBean(String selectPhaseVBByPublicParamBean) {
        this.selectPhaseVBByPublicParamBean = selectPhaseVBByPublicParamBean;
    }

    public String getSelectPhaseVCByPublicParamBean() {
        return selectPhaseVCByPublicParamBean;
    }

    public void setSelectPhaseVCByPublicParamBean(String selectPhaseVCByPublicParamBean) {
        this.selectPhaseVCByPublicParamBean = selectPhaseVCByPublicParamBean;
    }

    public String getSelectLineVABByPublicParamBean() {
        return selectLineVABByPublicParamBean;
    }

    public void setSelectLineVABByPublicParamBean(String selectLineVABByPublicParamBean) {
        this.selectLineVABByPublicParamBean = selectLineVABByPublicParamBean;
    }

    public String getSelectLineVBCByPublicParamBean() {
        return selectLineVBCByPublicParamBean;
    }

    public void setSelectLineVBCByPublicParamBean(String selectLineVBCByPublicParamBean) {
        this.selectLineVBCByPublicParamBean = selectLineVBCByPublicParamBean;
    }

    public String getSelectLineVCAByPublicParamBean() {
        return selectLineVCAByPublicParamBean;
    }

    public void setSelectLineVCAByPublicParamBean(String selectLineVCAByPublicParamBean) {
        this.selectLineVCAByPublicParamBean = selectLineVCAByPublicParamBean;
    }

    public String getSelectAmmeter() {
        return selectAmmeter;
    }

    public void setSelectAmmeter(String selectAmmeter) {
        this.selectAmmeter = selectAmmeter;
    }

    public String getSelectPowerFactorTotalByPublicParamBean() {
        return selectPowerFactorTotalByPublicParamBean;
    }

    public void setSelectPowerFactorTotalByPublicParamBean(String selectPowerFactorTotalByPublicParamBean) {
        this.selectPowerFactorTotalByPublicParamBean = selectPowerFactorTotalByPublicParamBean;
    }

    public String getSelectPowerFactorAByPublicParamBean() {
        return selectPowerFactorAByPublicParamBean;
    }

    public void setSelectPowerFactorAByPublicParamBean(String selectPowerFactorAByPublicParamBean) {
        this.selectPowerFactorAByPublicParamBean = selectPowerFactorAByPublicParamBean;
    }

    public String getSelectPowerFactorBByPublicParamBean() {
        return selectPowerFactorBByPublicParamBean;
    }

    public void setSelectPowerFactorBByPublicParamBean(String selectPowerFactorBByPublicParamBean) {
        this.selectPowerFactorBByPublicParamBean = selectPowerFactorBByPublicParamBean;
    }

    public String getSelectPowerFactorCByPublicParamBean() {
        return selectPowerFactorCByPublicParamBean;
    }

    public String getSelectDoorByPublicParamBean() {
        return selectDoorByPublicParamBean;
    }

    public void setSelectDoorByPublicParamBean(String selectDoorByPublicParamBean) {
        this.selectDoorByPublicParamBean = selectDoorByPublicParamBean;
    }

    public void setSelectPowerFactorCByPublicParamBean(String selectPowerFactorCByPublicParamBean) {
        this.selectPowerFactorCByPublicParamBean = selectPowerFactorCByPublicParamBean;
    }

    public String getSelectActivePowerByPublicParamBean() {
        return selectActivePowerByPublicParamBean;
    }

    public void setSelectActivePowerByPublicParamBean(String selectActivePowerByPublicParamBean) {
        this.selectActivePowerByPublicParamBean = selectActivePowerByPublicParamBean;
    }

    public String getSelectReactivePowerByPublicParamBean() {
        return selectReactivePowerByPublicParamBean;
    }

    public void setSelectReactivePowerByPublicParamBean(String selectReactivePowerByPublicParamBean) {
        this.selectReactivePowerByPublicParamBean = selectReactivePowerByPublicParamBean;
    }

    public String getSelectActiveByPublicParamBean() {
        return selectActiveByPublicParamBean;
    }

    public void setSelectActiveByPublicParamBean(String selectActiveByPublicParamBean) {
        this.selectActiveByPublicParamBean = selectActiveByPublicParamBean;
    }

    public String getSelectReactiveByPublicParamBean() {
        return selectReactiveByPublicParamBean;
    }

    public void setSelectReactiveByPublicParamBean(String selectReactiveByPublicParamBean) {
        this.selectReactiveByPublicParamBean = selectReactiveByPublicParamBean;
    }

    public String getSelectApparentByPublicParamBean() {
        return selectApparentByPublicParamBean;
    }

    public void setSelectApparentByPublicParamBean(String selectApparentByPublicParamBean) {
        this.selectApparentByPublicParamBean = selectApparentByPublicParamBean;
    }

    public String getSelectLineVByPublicParamBean() {
        return selectLineVByPublicParamBean;
    }

    public void setSelectLineVByPublicParamBean(String selectLineVByPublicParamBean) {
        this.selectLineVByPublicParamBean = selectLineVByPublicParamBean;
    }

    public String getSelectPhaseIByPublicParamBean() {
        return selectPhaseIByPublicParamBean;
    }

    public void setSelectPhaseIByPublicParamBean(String selectPhaseIByPublicParamBean) {
        this.selectPhaseIByPublicParamBean = selectPhaseIByPublicParamBean;
    }

    public String getSelectPhaseVByPublicParamBean() {
        return selectPhaseVByPublicParamBean;
    }

    public void setSelectPhaseVByPublicParamBean(String selectPhaseVByPublicParamBean) {
        this.selectPhaseVByPublicParamBean = selectPhaseVByPublicParamBean;
    }

    public String getSelectPowerFactorByPublicParamBean() {
        return selectPowerFactorByPublicParamBean;
    }

    public void setSelectPowerFactorByPublicParamBean(String selectPowerFactorByPublicParamBean) {
        this.selectPowerFactorByPublicParamBean = selectPowerFactorByPublicParamBean;
    }

    public String getSelectFreqByPublicParamBean() {
        return selectFreqByPublicParamBean;
    }

    public void setSelectFreqByPublicParamBean(String selectFreqByPublicParamBean) {
        this.selectFreqByPublicParamBean = selectFreqByPublicParamBean;
    }

    public String getSelectRealtimeFlowByPublicParamBean() {
        return selectRealtimeFlowByPublicParamBean;
    }

    public void setSelectRealtimeFlowByPublicParamBean(String selectRealtimeFlowByPublicParamBean) {
        this.selectRealtimeFlowByPublicParamBean = selectRealtimeFlowByPublicParamBean;
    }

    public String getSelectTotalFlowByPublicParamBean() {
        return selectTotalFlowByPublicParamBean;
    }

    public void setSelectTotalFlowByPublicParamBean(String selectTotalFlowByPublicParamBean) {
        this.selectTotalFlowByPublicParamBean = selectTotalFlowByPublicParamBean;
    }

    public String getSelectRayByPublicParamBean() {
        return selectRayByPublicParamBean;
    }

    public void setSelectRayByPublicParamBean(String selectRayByPublicParamBean) {
        this.selectRayByPublicParamBean = selectRayByPublicParamBean;
    }

    public String getSelectPressByPublicParamBean() {
        return selectPressByPublicParamBean;
    }

    public void setSelectPressByPublicParamBean(String selectPressByPublicParamBean) {
        this.selectPressByPublicParamBean = selectPressByPublicParamBean;
    }

    public String getSelectSmokeByPublicParamBean() {
        return selectSmokeByPublicParamBean;
    }

    public void setSelectSmokeByPublicParamBean(String selectSmokeByPublicParamBean) {
        this.selectSmokeByPublicParamBean = selectSmokeByPublicParamBean;
    }

    public String getSelectWaterByPublicParamBean() {
        return selectWaterByPublicParamBean;
    }

    public String getMachineCycle() {
        return machineCycle;
    }

    public void setMachineCycle(String machineCycle) {
        this.machineCycle = machineCycle;
    }

    public String getSelectDataRage() {
        return selectDataRage;
    }

    public void setSelectDataRage(String selectDataRage) {
        this.selectDataRage = selectDataRage;
    }

    public void setSelectWaterByPublicParamBean(String selectWaterByPublicParamBean) {
        this.selectWaterByPublicParamBean = selectWaterByPublicParamBean;
    }

    public String getSelectPM25ByPublicParamBean() {
        return selectPM25ByPublicParamBean;
    }

    public void setSelectPM25ByPublicParamBean(String selectPM25ByPublicParamBean) {
        this.selectPM25ByPublicParamBean = selectPM25ByPublicParamBean;
    }

    public String getSelectPM10ByPublicParamBean() {
        return selectPM10ByPublicParamBean;
    }

    public void setSelectPM10ByPublicParamBean(String selectPM10ByPublicParamBean) {
        this.selectPM10ByPublicParamBean = selectPM10ByPublicParamBean;
    }

    public String getSelectTempByPublicParamBean() {
        return selectTempByPublicParamBean;
    }

    public void setSelectTempByPublicParamBean(String selectTempByPublicParamBean) {
        this.selectTempByPublicParamBean = selectTempByPublicParamBean;
    }

    public String getSaveOrUpdateAir() {
        return saveOrUpdateAir;
    }

    public void setSaveOrUpdateAir(String saveOrUpdateAir) {
        this.saveOrUpdateAir = saveOrUpdateAir;
    }

    public String getSaveOrUpdateAmmeter() {
        return saveOrUpdateAmmeter;
    }

    public void setSaveOrUpdateAmmeter(String saveOrUpdateAmmeter) {
        this.saveOrUpdateAmmeter = saveOrUpdateAmmeter;
    }

    public String getSaveOrUpdateAtmos() {
        return saveOrUpdateAtmos;
    }

    public void setSaveOrUpdateAtmos(String saveOrUpdateAtmos) {
        this.saveOrUpdateAtmos = saveOrUpdateAtmos;
    }

    public String getSaveOrUpdateElecFreq() {
        return saveOrUpdateElecFreq;
    }

    public void setSaveOrUpdateElecFreq(String saveOrUpdateElecFreq) {
        this.saveOrUpdateElecFreq = saveOrUpdateElecFreq;
    }

    public String getSaveOrUpdateElecLineV() {
        return saveOrUpdateElecLineV;
    }

    public void setSaveOrUpdateElecLineV(String saveOrUpdateElecLineV) {
        this.saveOrUpdateElecLineV = saveOrUpdateElecLineV;
    }

    public String getSaveOrUpdatePhaseI() {
        return saveOrUpdatePhaseI;
    }

    public void setSaveOrUpdatePhaseI(String saveOrUpdatePhaseI) {
        this.saveOrUpdatePhaseI = saveOrUpdatePhaseI;
    }

    public String getSaveOrUpdatePhaseV() {
        return saveOrUpdatePhaseV;
    }

    public void setSaveOrUpdatePhaseV(String saveOrUpdatePhaseV) {
        this.saveOrUpdatePhaseV = saveOrUpdatePhaseV;
    }

    public String getSaveOrUpdatePower() {
        return saveOrUpdatePower;
    }

    public void setSaveOrUpdatePower(String saveOrUpdatePower) {
        this.saveOrUpdatePower = saveOrUpdatePower;
    }

    public String getSaveOrUpdatePowerFactor() {
        return saveOrUpdatePowerFactor;
    }

    public void setSaveOrUpdatePowerFactor(String saveOrUpdatePowerFactor) {
        this.saveOrUpdatePowerFactor = saveOrUpdatePowerFactor;
    }

    public String getSaveOrUpdateHum() {
        return saveOrUpdateHum;
    }

    public void setSaveOrUpdateHum(String saveOrUpdateHum) {
        this.saveOrUpdateHum = saveOrUpdateHum;
    }

    public String getSaveOrUpdateRay() {
        return saveOrUpdateRay;
    }

    public void setSaveOrUpdateRay(String saveOrUpdateRay) {
        this.saveOrUpdateRay = saveOrUpdateRay;
    }

    public String getSaveOrUpdateSmoke() {
        return saveOrUpdateSmoke;
    }

    public void setSaveOrUpdateSmoke(String saveOrUpdateSmoke) {
        this.saveOrUpdateSmoke = saveOrUpdateSmoke;
    }

    public String getSaveOrUpdateWater() {
        return saveOrUpdateWater;
    }

    public void setSaveOrUpdateWater(String saveOrUpdateWater) {
        this.saveOrUpdateWater = saveOrUpdateWater;
    }

    public String getSaveOrUpdateWaterNjyc() {
        return saveOrUpdateWaterNjyc;
    }

    public void setSaveOrUpdateWaterNjyc(String saveOrUpdateWaterNjyc) {
        this.saveOrUpdateWaterNjyc = saveOrUpdateWaterNjyc;
    }

    public String getSelectHumByPublicParamBean() {
        return selectHumByPublicParamBean;
    }

    public void setSelectHumByPublicParamBean(String selectHumByPublicParamBean) {
        this.selectHumByPublicParamBean = selectHumByPublicParamBean;
    }

    public String getSelectCoByPublicParamBean() {
        return selectCoByPublicParamBean;
    }

    public void setSelectCoByPublicParamBean(String selectCoByPublicParamBean) {
        this.selectCoByPublicParamBean = selectCoByPublicParamBean;
    }

    public String getSelectCo2ByPublicParamBean() {
        return selectCo2ByPublicParamBean;
    }

    public void setSelectCo2ByPublicParamBean(String selectCo2ByPublicParamBean) {
        this.selectCo2ByPublicParamBean = selectCo2ByPublicParamBean;
    }

    public String getSelectHchoByPublicParamBean() {
        return selectHchoByPublicParamBean;
    }

    public void setSelectHchoByPublicParamBean(String selectHchoByPublicParamBean) {
        this.selectHchoByPublicParamBean = selectHchoByPublicParamBean;
    }

    public String getSelectTvocByPublicParamBean() {
        return selectTvocByPublicParamBean;
    }

    public void setSelectTvocByPublicParamBean(String selectTvocByPublicParamBean) {
        this.selectTvocByPublicParamBean = selectTvocByPublicParamBean;
    }

    public String getSelectLargestDemandAByPublicParamBean() {
        return selectLargestDemandAByPublicParamBean;
    }

    public void setSelectLargestDemandAByPublicParamBean(String selectLargestDemandAByPublicParamBean) {
        this.selectLargestDemandAByPublicParamBean = selectLargestDemandAByPublicParamBean;
    }

    public String getSelectLargestDemandRByPublicParamBean() {
        return selectLargestDemandRByPublicParamBean;
    }

    public void setSelectLargestDemandRByPublicParamBean(String selectLargestDemandRByPublicParamBean) {
        this.selectLargestDemandRByPublicParamBean = selectLargestDemandRByPublicParamBean;
    }

    public String getSelectLargestDemandTByPublicParamBean() {
        return selectLargestDemandTByPublicParamBean;
    }

    public void setSelectLargestDemandTByPublicParamBean(String selectLargestDemandTByPublicParamBean) {
        this.selectLargestDemandTByPublicParamBean = selectLargestDemandTByPublicParamBean;
    }

}
