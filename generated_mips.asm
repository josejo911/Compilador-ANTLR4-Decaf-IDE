# MIPS autogenerado.
# IntelliJO IDEA, Implementacion de DECAF con Java, ANTLR4 y el framework de VAADIN


.text
.globl main
main:
	li $t0, 60
	move $a0, $t0
	sw $a0, n_factorial
	jal factorial
	move $s0, $v0


	# ---------- Exit ----------
	li $v0, 10
	syscall


factorial:
	addi $sp, $sp, -4				# Adjust stack pointer
	sw $s0, 0($sp)					# Save reg
	addi $sp, $sp, -4				# Adjust stack pointer
	sw $s1, 0($sp)					# Save reg
	addi $sp, $sp, -4				# Adjust stack pointer
	sw $s2, 0($sp)					# Save reg
	addi $sp, $sp, -4				# Adjust stack pointer
	sw $s3, 0($sp)					# Save reg
	li $s0, 1
	sw $s0, ans_factorial				# str data
	lw $t0, n_factorial				# ld data n_factorial
	move $s1, $t0
	sw $s1, i_factorial				# str data
	_L0:
	li $t1, 1
	lw $t2, i_factorial				# ld data i_factorial
	slt $t0, $t1, $t2
	blez $t0, _L1
	lw $t2, ans_factorial				# ld data ans_factorial
	lw $t1, i_factorial				# ld data i_factorial
	mult $t2, $t1
	mflo $s2
	sw $s2, ans_factorial				# str data
	lw $t1, i_factorial				# ld data i_factorial
	li $t2, 1
	sub $s3, $t1, $t2
	sw $s3, i_factorial				# str data
	b _L0
	_L1:
	li $v0, 1					# Print call
	lw $a0, ans_factorial
	syscall
	lw $t0, ans_factorial				# ld data ans_factorial
	move $t1, $t0
	lw $t3, ans_factorial				# ld data ans_factorial
	move $t2, $t3
	move $v0, $t1					# Return
	lw $s4, 0($sp)					# Restore reg
	addi $sp, $sp, 4				# Adjust stack pointer
	lw $s3, 0($sp)					# Restore reg
	addi $sp, $sp, 4				# Adjust stack pointer
	lw $s2, 0($sp)					# Restore reg
	addi $sp, $sp, 4				# Adjust stack pointer
	lw $s1, 0($sp)					# Restore reg
	addi $sp, $sp, 4				# Adjust stack pointer
	jr $ra							# Jump to addr stored in $ra


# ---------- data section ----------
.data
n_factorial:				.word 0
ans_factorial:				.word 0
i_factorial:				.word 0
