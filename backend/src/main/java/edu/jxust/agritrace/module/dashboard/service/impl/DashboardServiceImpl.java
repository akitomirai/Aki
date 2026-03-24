package edu.jxust.agritrace.module.dashboard.service.impl;

import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.dashboard.service.DashboardService;
import edu.jxust.agritrace.module.dashboard.vo.DashboardOverviewVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final BatchService batchService;

    public DashboardServiceImpl(BatchService batchService) {
        this.batchService = batchService;
    }

    @Override
    public DashboardOverviewVO getOverview() {
        List<String> focus = List.of(
                "企业建档要尽量一次录完，避免重复跳转。",
                "批次详情要像工作台，集中查看状态、记录、质检和二维码。",
                "公开追溯页只展示消费者关心的信息，不回传后台字段。"
        );
        var batches = batchService.listBatches();
        int published = (int) batches.stream().filter(batch -> BatchStatus.PUBLISHED.name().equals(batch.status())).count();
        int draft = (int) batches.stream().filter(batch -> BatchStatus.DRAFT.name().equals(batch.status())).count();
        int risk = (int) batches.stream().filter(batch -> List.of(BatchStatus.FROZEN.name(), BatchStatus.RECALLED.name()).contains(batch.status())).count();
        return new DashboardOverviewVO(
                batches.size(),
                published,
                draft,
                risk,
                "当前主链路聚焦为：企业建档 -> 批次创建 -> 过程记录 -> 质检上传 -> 二维码生成 -> 发布 -> 扫码查看。",
                focus
        );
    }
}
