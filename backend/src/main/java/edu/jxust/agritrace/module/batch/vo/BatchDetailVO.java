package edu.jxust.agritrace.module.batch.vo;

import lombok.Data;
import edu.jxust.agritrace.module.trace.vo.TraceNodeVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次详情 VO
 */
@Data
public class BatchDetailVO {

    private Long id;
    private String batchCode;

    private Long productId;
    private String productName;
    private String productCategory;
    private String productSpec;
    private String productUnit;

    private Long companyId;
    private String creatorCompanyName;
    private String originPlace;
    private LocalDate startDate;

    private String status;
    private String regulationStatus;

    private String remark;
    private String publicRemark;
    private String internalRemark;
    private String statusReason;

    private LocalDateTime publishedAt;
    private LocalDateTime frozenAt;
    private LocalDateTime recalledAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    private List<BatchParticipantVO> participants;

    private List<TraceNodeVO> nodes;
}
