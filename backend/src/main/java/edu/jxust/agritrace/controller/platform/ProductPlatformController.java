package edu.jxust.agritrace.controller.platform;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.product.dto.ProductCreateDTO;
import edu.jxust.agritrace.module.product.dto.ProductUpdateDTO;
import edu.jxust.agritrace.module.product.service.ProductService;
import edu.jxust.agritrace.module.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台端-产品管理接口
 */
@Tag(name = "平台端-产品管理")
@RestController
@RequestMapping("/api/platform/product")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class ProductPlatformController {

    private final ProductService productService;

    public ProductPlatformController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 新增产品
     */
    @Operation(summary = "新增产品")
    @OperationLogAnnotation(
            module = "PRODUCT",
            action = "CREATE_PRODUCT",
            targetType = "BASE_PRODUCT"
    )
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductCreateDTO dto) {
        Long id = productService.create(dto);
        return Result.ok(id);
    }

    /**
     * 查询产品列表
     */
    @Operation(summary = "查询产品列表")
    @GetMapping("/list")
    public Result<List<ProductVO>> list() {
        return Result.ok(productService.list());
    }

    /**
     * 查询产品详情
     */
    @Operation(summary = "查询产品详情")
    @GetMapping("/detail/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    /**
     * 修改产品
     */
    @Operation(summary = "修改产品")
    @OperationLogAnnotation(
            module = "PRODUCT",
            action = "UPDATE_PRODUCT",
            targetType = "BASE_PRODUCT"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateDTO dto) {
        productService.update(id, dto);
        return Result.ok();
    }
}