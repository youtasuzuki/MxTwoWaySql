update "myfirstmodule$employee"
set
	"intval"="intval" + 1,
	"decval"="decval" + 2.0
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
