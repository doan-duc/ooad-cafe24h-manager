package dao;

import java.util.List;

import model.LookupItem;
import model.MenuItem;

/** Defines data access operations for menu items and categories. */
public interface IMenuDao {

    List<MenuItem> list(String keyword);

    List<LookupItem> categories();

    void saveCategory(LookupItem item, boolean insert);

    void insert(MenuItem item);

    void update(MenuItem item);
}
