SELECT 	t.symbol, i.*, 
	DATEDIFF(NOW(), i.buy_dt) as DaysSinceBUY, 
	DATEDIFF(i.sell_dt_limit, NOW()) as DaysToSell
FROM 	investments i, 
	triggers t 
WHERE 	i.trigger_id = t.id;
