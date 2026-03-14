package edu.jxust.agritrace.module.product.service;

import edu.jxust.agritrace.module.product.dto.ProductCreateDTO;
import edu.jxust.agritrace.module.product.dto.ProductUpdateDTO;
import edu.jxust.agritrace.module.product.vo.ProductVO;

import java.util.List;

/**
 * 产品业务逻辑接口
 */
public interface ProductService {

    /**
     * 新增产品
     */
    Long create(ProductCreateDTO dto);

    /**
     * 查询产品列表
     */
    List<ProductVO> list();

    /**
     * 查询产品详情
     */
    ProductVO getById(Long id);

    /**
     * 修改产品
     */
    void update(Long id, ProductUpdateDTO dto);
}
