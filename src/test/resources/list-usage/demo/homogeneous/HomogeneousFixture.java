package demo.homogeneous;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class HomogeneousFixture {

    private ArrayList copyAccountsToDetails(Collection accounts) {
        ArrayList detailsList = new ArrayList();
        Iterator i = accounts.iterator();
        while (i.hasNext()) {
            AccountDetails details = new AccountDetails("id-1");
            detailsList.add(details);
        }
        return detailsList;
    }

    static final class AccountDetails {
        final String id;

        AccountDetails(String id) {
            this.id = id;
        }
    }
}
