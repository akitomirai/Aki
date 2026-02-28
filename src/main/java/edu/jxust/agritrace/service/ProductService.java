package edu.jxust.agritrace.service;

import edu.jxust.agritrace.common.PageResult;
import edu.jxust.agritrace.dto.ProductCreateDTO;
import edu.jxust.agritrace.dto.ProductQueryDTO;
import edu.jxust.agritrace.dto.PageRequestDTO;
import edu.jxust.agritrace.vo.ProductVO;

import java.util.List;

public interface ProductService {

    ProductVO create(ProductCreateDTO dto);

    List<ProductVO> list(ProductQueryDTO queryDTO);

    PageResult<ProductVO> page(ProductQueryDTO queryDTO, PageRequestDTO pageRequestDTO);
}
