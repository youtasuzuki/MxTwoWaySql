
select
	emp."id" as IdValue
	-- ,dept."id" as IdValueOfDept
from
	"myfirstmodule$employee" emp
	-- left outer join "myfirstmodule$employee_department" emp_dept on emp_dept."myfirstmodule$employeeid"=emp."id"
	-- left outer join "myfirstmodule$department" dept on dept."id"=emp_dept."myfirstmodule$departmentid"
/*BEGIN*/
where
	/*IF pmb.Name != null*/
	emp."name" like /*pmb.Name*/'bbb'
	/*END*/
/*END*/
order by emp."name" desc
