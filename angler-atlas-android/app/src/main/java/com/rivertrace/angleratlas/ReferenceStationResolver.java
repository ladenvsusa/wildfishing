package com.rivertrace.angleratlas;

/** Small deterministic mapping used before a user manually overrides the station. */
public final class ReferenceStationResolver {
    private ReferenceStationResolver() {}

    public static String forCity(String city) {
        if (city == null) return "汉口";
        if (city.contains("武汉")) return "汉口";
        if (city.contains("宜昌")) return "宜昌";
        if (city.contains("荆州")) return "沙市";
        if (city.contains("九江")) return "九江";
        if (city.contains("岳阳")) return "莲花塘";
        return "汉口";
    }
}
