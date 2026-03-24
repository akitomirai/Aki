package edu.jxust.agritrace.module.publictrace.vo;

public record PublicTimelineItemVO(
        String stageName,
        String title,
        String time,
        String location,
        String summary
) {
}
