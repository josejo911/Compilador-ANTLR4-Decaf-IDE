_factorial:
	BeginFunc 16;
	ans = 1;
	i = n;

	StartWhile:

	_L0:
	_t0 = 1<i;
	Ifz _t0 Goto _L1;
	ans = ans*i;
	i = i-1;
	Goto _L0:

	_L1:
	PushParam ans;
	LCall print;
	PopParams 1;
	_t1 = ans;
	_t2 = ans;
	Return _t1;
	EndFunc;
main:
	BeginFunc 8;
	_t0 = 60;
	PushParam _t0;
	LCall _factorial;
	PopParams 4;
	EndFunc;
