-- #MyFirstModule.EmployeeTwoWaySqlResult#
-- @TwoWaySQL@ 
select
	"address","ishoge","intval","decval","name","birthdate","id" as IdValue
from
	"myfirstmodule$employee"
/*BEGIN*/
where
	/*IF pmb.NameList != null*/
	"name" in /*pmb.NameList*/('社員_0','社員_1','社員_2')
	/*END*/
/*END*/
