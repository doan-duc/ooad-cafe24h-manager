package controller;

import dao.IOperationsDao;
import dao.OperationsDao;
import model.OperationsSnapshot;
import security.Authorization;
import security.Permission;
import security.Session;

public final class OperationsController {
    private final IOperationsDao dao;

    public OperationsController() {
        this(new OperationsDao());
    }

    public OperationsController(IOperationsDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Lấy ảnh chụp nhanh tình hình quản lý
    public OperationsSnapshot snapshot() {
        if (Session.currentUser() == null) {
            throw new SecurityException("Chưa đăng nhập.");
        }
        return dao.snapshot(Authorization.can(
                Session.currentUser(), Permission.REPORT_VIEW));
    }
}
