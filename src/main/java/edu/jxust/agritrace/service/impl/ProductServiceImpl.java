package edu.jxust.agritrace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jxust.agritrace.common.PageResult;
import edu.jxust.agritrace.dto.PageRequestDTO;
import edu.jxust.agritrace.dto.ProductCreateDTO;
import edu.jxust.agritrace.dto.ProductQueryDTO;
import edu.jxust.agritrace.entity.Product;
import edu.jxust.agritrace.mapper.ProductMapper;
import edu.jxust.agritrace.service.ProductService;
import edu.jxust.agritrace.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public ProductVO create(ProductCreateDTO dto) {
        Product p = new Product();
        p.setProductName(dto.getProductName());
        p.setCategory(dto.getCategory());
        p.setOriginPlace(dto.getOriginPlace());
        p.setDescription(dto.getDescription());
        p.setCreateTime(LocalDateTime.now());

        productMapper.insert(p);
        return toVO(p);
    }

    @Override
    public List<ProductVO> list(ProductQueryDTO queryDTO) {
        LambdaQueryWrapper<Product> qw = buildQuery(queryDTO);
        List<Product> list = productMapper.selectList(qw);

        List<ProductVO> vos = new ArrayList<>();
        for (Product p : list) {
            vos.add(toVO(p));
        }
        return vos;
    }

    @Override
    public PageResult<ProductVO> page(ProductQueryDTO queryDTO, PageRequestDTO pageRequestDTO) {
        int pageNo = pageRequestDTO.getPage();
        int pageSize = pageRequestDTO.getSize();

        if (pageNo <= 0) pageNo = 1;
        if (pageSize <= 0) pageSize = 10;

        LambdaQueryWrapper<Product> qw = buildQuery(queryDTO);
        qw.orderByDesc(Product::getCreateTime);

        IPage<Product> page = productMapper.selectPage(new Page<>(pageNo, pageSize), qw);

        List<ProductVO> records = new ArrayList<>();
        for (Product p : page.getRecords()) {
            records.add(toVO(p));
        }

        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    private LambdaQueryWrapper<Product> buildQuery(ProductQueryDTO queryDTO) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        if (queryDTO == null) {
            return qw;
        }

        if (StringUtils.hasText(queryDTO.getProductName())) {
            qw.like(Product::getProductName, queryDTO.getProductName().trim());
        }
        if (StringUtils.hasText(queryDTO.getCategory())) {
            qw.eq(Product::getCategory, queryDTO.getCategory().trim());
        }
        if (StringUtils.hasText(queryDTO.getOriginPlace())) {
            qw.like(Product::getOriginPlace, queryDTO.getOriginPlace().trim());
        }

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = queryDTO.getKeyword().trim();
            qw.and(w -> w.like(Product::getProductName, kw)
                    .or().like(Product::getCategory, kw)
                    .or().like(Product::getOriginPlace, kw)
                    .or().like(Product::getDescription, kw));
        }

        return qw;
    }

    private ProductVO toVO(Product p) {
        if (p == null) return null;
        ProductVO vo = new ProductVO();
        vo.setId(p.getId());
        vo.setProductName(p.getProductName());
        vo.setCategory(p.getCategory());
        vo.setOriginPlace(p.getOriginPlace());
        vo.setDescription(p.getDescription());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }
}
