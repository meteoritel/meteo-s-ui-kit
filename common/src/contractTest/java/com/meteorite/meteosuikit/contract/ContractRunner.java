package com.meteorite.meteosuikit.contract;

import java.util.LinkedHashMap;
import java.util.Map;

/** 可单跑的公开行为契约入口；每个契约自行建立和关闭其状态。 */
public final class ContractRunner {
    private ContractRunner() { }
    public static void main(String[] args) {
        Map<String, Runnable> contracts = new LinkedHashMap<>();
        contracts.put("theme", ThemeContract::run);
        contracts.put("geometry", GeometryContract::run);
        contracts.put("input", InputContract::run);
        contracts.put("scroll", ScrollContract::run);
        contracts.put("text", TextContract::run);
        contracts.put("tooltip", TooltipContract::run);
        contracts.put("root", RootContract::run);
        contracts.put("lightbox", LightboxContract::run);
        for (String name : args.length == 0 ? contracts.keySet().toArray(String[]::new) : args) {
            Runnable contract = contracts.get(name);
            if (contract == null) throw new IllegalArgumentException("Unknown contract: " + name);
            try { contract.run(); }
            catch (RuntimeException | AssertionError failure) { throw new AssertionError("Contract: " + name, failure); }
            System.out.println("PASS: " + name);
        }
    }
}
