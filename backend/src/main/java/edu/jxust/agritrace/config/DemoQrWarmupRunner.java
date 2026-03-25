package edu.jxust.agritrace.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.module.batch.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.batch.mapper.po.QrCodePO;
import edu.jxust.agritrace.module.batch.service.support.QrImageStorageService;
import edu.jxust.agritrace.module.batch.service.support.TraceLinkBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoQrWarmupRunner implements CommandLineRunner {

    private final QrCodeMapper qrCodeMapper;
    private final QrImageStorageService qrImageStorageService;
    private final TraceLinkBuilder traceLinkBuilder;

    public DemoQrWarmupRunner(
            QrCodeMapper qrCodeMapper,
            QrImageStorageService qrImageStorageService,
            TraceLinkBuilder traceLinkBuilder
    ) {
        this.qrCodeMapper = qrCodeMapper;
        this.qrImageStorageService = qrImageStorageService;
        this.traceLinkBuilder = traceLinkBuilder;
    }

    @Override
    public void run(String... args) {
        qrCodeMapper.selectList(new LambdaQueryWrapper<QrCodePO>().orderByAsc(QrCodePO::getId))
                .stream()
                .map(QrCodePO::getQrToken)
                .filter(token -> token != null && !token.isBlank())
                .forEach(token -> qrImageStorageService.ensureQrImage(token, traceLinkBuilder.buildPublicTraceUrl(token)));
    }
}
