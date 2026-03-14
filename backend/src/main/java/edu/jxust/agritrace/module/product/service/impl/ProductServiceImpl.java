package edu.jxust.agritrace.module.product.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.module.product.dto.ProductCreateDTO;
import edu.jxust.agritrace.module.product.dto.ProductUpdateDTO;
import edu.jxust.agritrace.module.product.entity.BaseProduct;
import edu.jxust.agritrace.module.product.mapper.ProductMapper;
import edu.jxust.agritrace.module.product.service.ProductService;
import edu.jxust.agritrace.module.product.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 产品业务逻辑实现
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    private static String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductCreateDTO dto) {
        dto.setName(normalizeName(dto.getName()));
        dto.setCategory(normalizeBlankToNull(dto.getCategory()));
        dto.setSpec(normalizeBlankToNull(dto.getSpec()));
        dto.setUnit(normalizeBlankToNull(dto.getUnit()));

        // 产品重复校验：按名称、规格、单位组合判断
        Long existingId = productMapper.selectIdByUniqueKey(dto.getName(), dto.getSpec(), dto.getUnit());
        if (existingId != null) {
            throw new BizException("该产品已存在");
        }

        BaseProduct product = new BaseProduct();
        BeanUtils.copyProperties(dto, product);
        product.setCreatedAt(LocalDateTime.now());
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    public List<ProductVO> list() {
        return productMapper.selectProductList();
    }

    @Override
    public List<ProductVO> listByCompanyId(Long companyId) {
        return productMapper.selectProductListByCompanyId(companyId);
    }

    @Override
    public ProductVO getById(Long id) {
        BaseProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("产品不存在");
        }

        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductUpdateDTO dto) {
        BaseProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("产品不存在");
        }

        dto.setName(normalizeName(dto.getName()));
        dto.setCategory(normalizeBlankToNull(dto.getCategory()));
        dto.setSpec(normalizeBlankToNull(dto.getSpec()));
        dto.setUnit(normalizeBlankToNull(dto.getUnit()));

        // 产品重复校验：按名称、规格、单位组合判断（排除自身）
        Long existingId = productMapper.selectIdByUniqueKey(dto.getName(), dto.getSpec(), dto.getUnit());
        if (existingId != null && !existingId.equals(id)) {
            throw new BizException("该产品已存在");
        }

        BeanUtils.copyProperties(dto, product);
        productMapper.updateById(product);
    }
}
