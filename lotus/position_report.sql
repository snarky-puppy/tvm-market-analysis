-- select t.symbol, i.buy_dt, i.buy_limit, i.sell_limit, i.qty, i.qty_filled, i.state, i.error_msg from investments i, triggers t where i.trigger_id = t.id order by i.buy_dt, state;
select t.symbol, i.*, DATEDIFF(NOW(), i.buy_dt) as DaysSinceBUY from investments i, triggers t where i.trigger_id = t.id;
