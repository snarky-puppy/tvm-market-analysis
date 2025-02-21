select t.* from triggers t, active_symbols s where s.symbol = t.symbol ORDER BY trigger_date;
