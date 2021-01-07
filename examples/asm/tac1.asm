.text
.globl main

main:
	lw $t0, b_main		# b
	lw $t1, c_main		# c
	add $t3, $t0, $t1	# _t0 = b + c
	lw $t1, d_main		# d
	add $s0, $t3, $t1	# a = _t0 + d
	sw $s0, a_main

	lw $t0, a_main
	mult $t0, $t0		# _t1 = a * a
	mflo $t0

	lw $t1, b_main		# b
	mult $t1, $t1		# _t2 = b * b
	mflo $t1

	add $s0, $t0, $t1	# b = _t1 + _t2
	sw $s0, b_main

	# -----------------------------------print stored values----------------------------------------
	li $v0, 4
	la $a0, msg
	syscall

	li $v0, 1		# print a
	lw $t0, a_main
	move $a0, $t0
	syscall

	li $v0, 4
	la $a0, msg
	syscall

	li $v0, 1		# print b
	lw $t0, b_main
	move $a0, $t0
	syscall

	# Exit
	li $v0, 10
	syscall



.data
a_main:		.word 0
b_main:		.word 0
c_main:		.word 0
d_main:		.word 0
msg:		.asciiz "Resultado es: "