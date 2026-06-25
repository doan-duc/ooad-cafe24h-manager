package dao;

import java.util.List;

import model.Area;
import model.RecipeLine;
import model.TableConfig;
import model.Voucher;

/** Defines data access operations for store configuration. */
public interface IConfigDao {

    List<Area> listAreas();

    void saveArea(Area area, boolean insert);

    List<TableConfig> listTables();

    void saveTable(TableConfig table, boolean insert);

    List<Voucher> listVouchers();

    void saveVoucher(Voucher voucher, boolean insert);

    List<RecipeLine> listRecipe(String maMon);

    void saveRecipe(RecipeLine line);

    void deleteRecipe(String maMon, String maNL);
}
