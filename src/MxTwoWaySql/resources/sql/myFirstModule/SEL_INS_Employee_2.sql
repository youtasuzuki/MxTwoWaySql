-- #MyFirstModule.Employee_2#
select
	Name, Address, BirthDate, IsHoge, DecVal, IntVal
from
	"myfirstmodule$employee"
where
	name like /*pmb.Name*/'%100%'
