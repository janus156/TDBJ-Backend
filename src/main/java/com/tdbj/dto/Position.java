package com.tdbj.dto;

import lombok.Data;

/**
 * 获得经度和纬度来定位
 */

@Data
public class Position {
    private Double latitude;
    private Double longitude;
}
