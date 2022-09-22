package be.isach.ultracosmetics.worldguard;

import com.sk89q.worldguard.protection.flags.StateFlag;

public enum UCFlag {
    COSMETICS(new StateFlag("uc-cosmetics", true)),
    TREASURE(new StateFlag("uc-treasurechest", true)),
    AFFECT_PLAYERS(new StateFlag("uc-affect-players", true)),
    ;

    private final StateFlag flag;

    private UCFlag(StateFlag flag) {
        this.flag = flag;
    }

    public StateFlag getFlag() {
        return flag;
    }
}
