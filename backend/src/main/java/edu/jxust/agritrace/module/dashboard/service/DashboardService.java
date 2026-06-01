package edu.jxust.agritrace.module.dashboard.service;

import edu.jxust.agritrace.module.dashboard.vo.DashboardOverviewVO;
import edu.jxust.agritrace.module.dashboard.vo.DataBackupSnapshotVO;

public interface DashboardService {

    DashboardOverviewVO getOverview();

    DataBackupSnapshotVO createBackupSnapshot();
}
