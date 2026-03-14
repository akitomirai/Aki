package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchParticipantSaveDTO {

    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    private List<BatchParticipantItemDTO> participants;
}
