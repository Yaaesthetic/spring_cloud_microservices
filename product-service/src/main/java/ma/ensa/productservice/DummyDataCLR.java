package ma.ensa.productservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class DummyDataCLR implements CommandLineRunner {

    @Override
    public void run(String... strings) throws Exception {
        Stream.of("Pencil", "Book", "Eraser").forEach(s -> productRepository.save(new
                Product(s)));
        productRepository.findAll().forEach(s -> System.out.println(s.getName()));

    }

    @Autowired
    private ProductRepository productRepository;
}