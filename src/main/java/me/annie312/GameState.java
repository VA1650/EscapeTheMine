package me.annie312;

public enum GameState {
    WAITING,   // Ожидание игроков
    STARTING,  // Идет отсчет в лобби
    INGAME,    // Сама игра (процесс)
    ENDING     // Финал, титры, очистка
}
