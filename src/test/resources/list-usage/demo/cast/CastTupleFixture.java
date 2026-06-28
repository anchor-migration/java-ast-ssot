package demo.cast;

import java.util.ArrayList;

public class CastTupleFixture {

    void consume(ArrayList row) {
        String code = (String) row.get(0);
        Integer status = (Integer) row.get(1);
    }
}
