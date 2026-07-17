-- #MyFirstModule.EmployeeTwoWaySqlResult#
select
	"id" as IdValue
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
--	and "birthdate" = /*pmb.BirthDate*/'2017-05-01'
	/*END*/
/*END*/
