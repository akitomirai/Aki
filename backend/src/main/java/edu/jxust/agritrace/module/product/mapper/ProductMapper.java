package edu.jxust.agritrace.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.product.entity.BaseProduct;
import edu.jxust.agritrace.module.product.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 产品数据库操作接口
 */
@Mapper
public interface ProductMapper extends BaseMapper<BaseProduct> {

    /**
     * 查询产品列表
     */
    @Select("""
            SELECT
                id,
                name,
                category,
                spec,
                unit,
                created_at
            FROM base_product
            ORDER BY created_at DESC, id DESC
            """)
    List<ProductVO> selectProductList();

    @Select("""
            SELECT
                p.id,
                p.name,
                p.category,
                p.spec,
                p.unit,
                p.created_at
            FROM base_product p
            WHERE EXISTS (
                SELECT 1
                FROM trace_batch tb
                WHERE tb.product_id = p.id
                  AND tb.company_id = #{companyId}
            )
            ORDER BY p.created_at DESC, p.id DESC
            """)
    List<ProductVO> selectProductListByCompanyId(@Param("companyId") Long companyId);

    /**
     * 根据名称、规格、单位查询产品ID（用于唯一性校验）
     */
    @Select("""
            SELECT id
            FROM base_product
            WHERE name = #{name}
            AND (
                (spec = #{spec})
                OR (spec IS NULL AND #{spec} IS NULL)
            )
            AND (
                (unit = #{unit})
                OR (unit IS NULL AND #{unit} IS NULL)
            )
            LIMIT 1
            """)
    Long selectIdByUniqueKey(@Param("name") String name,
                             @Param("spec") String spec,
                             @Param("unit") String unit);
}
