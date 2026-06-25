package ui.panel;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import controller.EmployeeController;
import model.Employee;
import model.LookupItem;
import security.Authorization;
import security.Permission;
import security.Session;
import ui.Ui;

public final class EmployeePanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại danh sách nhân viên mỗi khi mở lại tab
    @Override
    public void onPageShown() {
        refresh();
    }

    private final EmployeeController controller = new EmployeeController();
    private final JTextField search = Ui.field(18);
    private final DefaultTableModel model = Ui.readOnlyModel(
            "Mã", "Họ tên", "Điện thoại", "Email", "Vai trò", "Trạng thái");
    private final JTable table = Ui.table(model);
    private List<Employee> employees = List.of();

    public EmployeePanel() {
        super(new BorderLayout());
        boolean canManage = Authorization.can(
                Session.currentUser(), Permission.EMPLOYEE_MANAGE);
        JButton find = Ui.secondaryButton("Tìm");
        JButton add = Ui.primaryButton("Thêm nhân viên");
        add.setEnabled(canManage);
        find.addActionListener(event -> refresh());
        search.addActionListener(event -> refresh());
        add.addActionListener(event -> edit(null));
        JPanel page = Ui.page(
                "Nhân viên",
                canManage
                        ? "Quản lý tài khoản, vai trò và trạng thái làm việc."
                        : "Theo dõi danh sách nhân viên; quyền chỉnh sửa thuộc Chủ quán.",
                Ui.toolbar(search, find, add));

        JPanel card = Ui.card();
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        JButton edit = Ui.secondaryButton("Chỉnh sửa");
        JButton reset = Ui.secondaryButton("Đặt lại mật khẩu");
        edit.setEnabled(canManage);
        reset.setEnabled(canManage);
        edit.addActionListener(event -> {
            Employee employee = selected();
            if (employee != null) {
                edit(employee);
            }
        });
        reset.addActionListener(event -> resetPassword());
        card.add(Ui.toolbar(edit, reset), BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        add(page);
        refresh();
    }

    private void refresh() {
        try {
            employees = controller.list(search.getText());
            model.setRowCount(0);
            for (Employee employee : employees) {
                model.addRow(new Object[] {
                        employee.maNV(), employee.hoTen(), employee.soDienThoai(),
                        employee.email(), employee.tenVaiTro(), employee.trangThai()
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private Employee selected() {
        try {
            return employees.get(Ui.selectedModelRow(table));
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            return null;
        }
    }

    private void edit(Employee current) {
        boolean insert = current == null;
        try {
            List<LookupItem> roles = controller.roles();
            JTextField code = Ui.field(18);
            JTextField name = Ui.field(18);
            JTextField phone = Ui.field(18);
            JTextField email = Ui.field(18);
            JComboBox<LookupItem> role =
                    new JComboBox<>(roles.toArray(LookupItem[]::new));
            JComboBox<String> status = new JComboBox<>(
                    new String[] {"Active", "Inactive"});
            JPasswordField password = new JPasswordField(18);
            if (current != null) {
                code.setText(current.maNV());
                code.setEnabled(false);
                name.setText(current.hoTen());
                phone.setText(current.soDienThoai());
                email.setText(current.email());
                status.setSelectedItem(current.trangThai());
                selectRole(role, current.maVaiTro());
            }
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Mã nhân viên", code);
            addRow(form, 1, "Họ tên", name);
            addRow(form, 2, "Số điện thoại", phone);
            addRow(form, 3, "Email", email);
            addRow(form, 4, "Vai trò", role);
            addRow(form, 5, "Trạng thái", status);
            if (insert) {
                addRow(form, 6, "Mật khẩu ban đầu", password);
            }
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, insert ? "Thêm nhân viên" : "Sửa nhân viên",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    LookupItem selectedRole = (LookupItem) role.getSelectedItem();
                    Employee employee = new Employee(
                            code.getText().trim(), name.getText().trim(), phone.getText().trim(),
                            blankToNull(email.getText()), null, (String) status.getSelectedItem(),
                            selectedRole.id(), selectedRole.name());
                    if (insert) {
                        controller.create(employee, password.getPassword());
                    } else {
                        controller.update(employee);
                    }
                    refresh();
                    break;
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void resetPassword() {
        Employee employee = selected();
        if (employee == null) {
            return;
        }
        JPasswordField password = new JPasswordField(18);
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        addRow(form, 0, "Nhân viên", new JLabel(employee.hoTen()));
        addRow(form, 1, "Mật khẩu mới", password);
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, form, "Đặt lại mật khẩu",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                controller.resetPassword(employee.maNV(), password.getPassword());
                Ui.info(this, "Đã cập nhật mật khẩu.");
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private static void selectRole(JComboBox<LookupItem> box, String id) {
        for (int index = 0; index < box.getItemCount(); index++) {
            if (box.getItemAt(index).id().equals(id)) {
                box.setSelectedIndex(index);
                return;
            }
        }
    }

    private static void addRow(
            JPanel form, int row, String label, java.awt.Component component) {
        form.add(new JLabel(label), Ui.gbc(0, row));
        form.add(component, Ui.gbc(1, row));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
