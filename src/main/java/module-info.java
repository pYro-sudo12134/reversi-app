module by.losik.reversi_player {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires org.slf4j;
    requires javafx.media;
    requires io.vertx.core;
    requires io.vertx.web;
    requires io.vertx.auth.jwt;
    requires io.vertx.auth.common;
    requires io.vertx.web.client;

    opens by.losik.reversi_player.controller to javafx.fxml;
    exports by.losik.reversi_player.exception;
    opens by.losik.reversi_player.exception to javafx.fxml;
    exports by.losik.reversi_player.helper;
    opens by.losik.reversi_player.helper to javafx.fxml;
    exports by.losik.reversi_player.entity;
    opens by.losik.reversi_player.entity to javafx.fxml;
    exports by.losik.reversi_player.verticles;
    opens by.losik.reversi_player.verticles to javafx.fxml;
    exports by.losik.reversi_player;
    opens by.losik.reversi_player to javafx.fxml;
    exports by.losik.reversi_player.controller;
}