package edu.jxust.agritrace.module.publictrace.vo;

public record PublicTimelineItemVO(
        String stageCode,
        String stageName,
        String title,
        String time,
        String location,
        String summary,
        String imageUrl
) {
}
