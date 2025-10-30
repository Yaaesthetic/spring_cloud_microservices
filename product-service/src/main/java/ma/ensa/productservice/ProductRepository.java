package ma.ensa.productservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product , Integer> {

//    @Query("select p from Product p where p.name like :name")
//    public Page<Product> productByName(@Param("name") String mc
//            , Pageable pageable);
    public Product findByName(String name);

}