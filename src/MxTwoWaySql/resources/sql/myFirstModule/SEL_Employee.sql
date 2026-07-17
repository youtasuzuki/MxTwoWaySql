-- #MyFirstModule.EmployeeTwoWaySqlResult#
-- @TwoWaySQL@ 
select
/*IF !pmb.IsJapanese*/
	"address","ishoge","intval","decval","name","birthdate","id" as IdValue
-- ELSE
--  "address" AS 住所, "ishoge" AS ほげかい, "decval" AS 整数変数, "decval" AS 十進変数, "name" AS 名前, "birthdate" AS 生年月日,"id" as IdValue
/*END*/
from
	"myfirstmodule$employee"
/*BEGIN*/
where
	/*IF pmb.Name != null*/
	"name" like /*pmb.Name*/'bbb'
	/*END*/
	/*IF pmb.Address != null*/
	and "address" like /*pmb.Address*/'aaa'
	/*END*/
	/*IF pmb.BirthDate != null*/
	and "birthdate" = /*pmb.BirthDate*/'2017-05-01'
	/*END*/
/*END*/
