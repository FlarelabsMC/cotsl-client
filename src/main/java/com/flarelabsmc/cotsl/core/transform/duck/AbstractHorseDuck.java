package com.flarelabsmc.cotsl.core.transform.duck;

public interface AbstractHorseDuck {
    float getVel();
    float getTurnRate();
    double getXV();
    double getYV();
    double getZV();
    void setVel(float speed);
    void setTurnRate(float turnRate);
    void setXV(double xv);
    void setYV(double yv);
    void setZV(double zv);
}
