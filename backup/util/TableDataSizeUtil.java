package com.wl4g.tools.hbase.phoenix.util;

public class TableDataSizeUtil {
    private static byte tb_air = 52;
    private static byte tb_ammeter = 52;
    private static byte tb_atmos = 44;
    private static byte tb_demand = 62;
    private static byte tb_elec_freq = 44;
    private static byte tb_elec_linev = 60;
    private static byte tb_elec_phasei = 60;
    private static byte tb_elec_phasev = 60;
    private static byte tb_elec_power = 126;
    private static byte tb_elec_powerfactor = 66;
    private static byte tb_freq_rate = 44;
    private static byte tb_harmonic_i = 70;
    private static byte tb_harmonic_v = 70;
    private static byte tb_hum = 52;
    private static byte tb_phase_v_rate = 61;
    private static byte tb_rate_i = 46;
    private static byte tb_rate_v = 46;
    private static byte tb_ray = 44;
    private static byte tb_smoke = 44;
    private static byte tb_water = 44;
    private static byte tb_water_njyc = 52;
    private static byte tb_heartbeat = 37;

    public static byte getByte(String tableName) {
        byte size = 0;
        if (tableName.equals("tb_air"))
            size = tb_air;
        else if (tableName.equals("tb_ammeter"))
            size = tb_ammeter;
        else if (tableName.equals("tb_atmos"))
            size = tb_atmos;
        else if (tableName.equals("tb_demand"))
            size = tb_demand;
        else if (tableName.equals("tb_elec_freq"))
            size = tb_elec_freq;
        else if (tableName.equals("tb_elec_linev"))
            size = tb_elec_linev;
        else if (tableName.equals("tb_elec_phasei"))
            size = tb_elec_phasei;
        else if (tableName.equals("tb_elec_phasev"))
            size = tb_elec_phasev;
        else if (tableName.equals("tb_elec_power"))
            size = tb_elec_power;
        else if (tableName.equals("tb_elec_powerfactor"))
            size = tb_elec_powerfactor;
        else if (tableName.equals("tb_freq_rate"))
            size = tb_freq_rate;
        else if (tableName.equals("tb_harmonic_i"))
            size = tb_harmonic_i;
        else if (tableName.equals("tb_harmonic_v"))
            size = tb_harmonic_v;
        else if (tableName.equals("tb_hum"))
            size = tb_hum;
        else if (tableName.equals("tb_phase_v_rate"))
            size = tb_phase_v_rate;
        else if (tableName.equals("tb_rate_i"))
            size = tb_rate_i;
        else if (tableName.equals("tb_rate_v"))
            size = tb_rate_v;
        else if (tableName.equals("tb_ray"))
            size = tb_ray;
        else if (tableName.equals("tb_smoke"))
            size = tb_smoke;
        else if (tableName.equals("tb_water"))
            size = tb_water;
        else if (tableName.equals("tb_water_njyc"))
            size = tb_water_njyc;
        else if (tableName.equals("tb_heartbeat")) {
            size = tb_heartbeat;
        }
        return size;
    }
}