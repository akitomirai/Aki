package edu.jxust.agritrace.dto;

import lombok.Data;

@Data
public class PageRequestDTO {
    private Integer page = 1;
    private Integer size = 10;
}
