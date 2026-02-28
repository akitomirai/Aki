package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.PageResult;
import edu.jxust.agritrace.common.Result;
import edu.jxust.agritrace.dto.PageRequestDTO;
import edu.jxust.agritrace.dto.ProductCreateDTO;
import edu.jxust.agritrace.dto.ProductQueryDTO;
import edu.jxust.agritrace.service.ProductService;
import edu.jxust.agritrace.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 创建产品（只收 DTO，只回 VO）
     */
    @PostMapping
    public Result<ProductVO> create(@RequestBody ProductCreateDTO dto) {
        return Result.ok(productService.create(dto));
    }

    /**
     * 列表查询（支持条件过滤）
     * 例：GET /api/products?productName=苹果&category=水果
     */
    @GetMapping
    public Result<List<ProductVO>> list(ProductQueryDTO queryDTO) {
        return Result.ok(productService.list(queryDTO));
    }

    /**
     * 分页查询
     * 例：GET /api/products/page?pageNo=1&pageSize=10&keyword=苹果
     */
    @GetMapping("/page")
    public Result<PageResult<ProductVO>> page(ProductQueryDTO queryDTO, PageRequestDTO pageRequestDTO) {
        return Result.ok(productService.page(queryDTO, pageRequestDTO));
    }
}
