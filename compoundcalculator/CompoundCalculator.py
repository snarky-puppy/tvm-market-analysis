######################################################################
# Import the necessary python modules
######################################################################

import numpy as np
import datetime
import Tkinter as tk
from tkFileDialog import askopenfilename
from tkFileDialog import askdirectory
import tkMessageBox
import os
import threading

######################################################################
# Set the transaction file
######################################################################

def select_file():
	
	global transact_filename
	
	transact_filename = askopenfilename(parent=root, title='Please select transaction file') # show an "Open" dialog box and return the path to the selected file
	transact_filename_var.set(transact_filename)

######################################################################
# Set output directory
######################################################################

def set_output_dir():
	
	global output_dir
	
	output_dir = askdirectory(parent=root, title='Please select output directory')
	output_dir_var.set(output_dir)

######################################################################
# Read the necessary columns from the transaction file
######################################################################

def read_transactions():
	
	####################
	# Define global variables:
	####################
	
	global transactions_good
	transactions_good = True
	
	####################
	# Read transactions from file:
	####################
	
	try:
		
		Labels, str_Date = np.loadtxt(transact_filename, dtype='str', usecols=(0,2), unpack=True, delimiter=',')
		Invest_Withdraw = np.loadtxt(transact_filename, usecols=(1,), delimiter=',')
		
		N = len(Labels)
		
		Invest_Date = np.empty(N, dtype='datetime64[D]')
		
		for n in xrange(N):
			
			dash1 = str_Date[n].find('/')
			dash2 = (dash1 + 1) + str_Date[n][dash1+1:].find('/')
			
			the_day = int(str_Date[n][:dash1])
			the_month = int(str_Date[n][dash1+1:dash2])
			the_year = int(str_Date[n][dash2+1:])
			
			Invest_Date[n] = datetime.date(the_year, the_month, the_day)
		
		return Labels, Invest_Withdraw, Invest_Date, N
	
	except:
		tkMessageBox.showerror("Invalid transaction file", "Please select an appropriate transaction file.")
		transactions_good = False

######################################################################
# Perform the compound calculation, and save the output
######################################################################

def compound_calculation(SB, MIpc, s, Labels, Invest_Withdraw, Invest_Date, N):
	
	####################
	# Calculate ROIs (as a fraction, not a percentage) for each investment, and store in the ROI dictionary:
	####################
	
	ROIs = {}
	
	unique_Labels = np.unique(Labels)
	M = len(unique_Labels)
	
	for m in xrange(M):
		where_Label = np.where(Labels==unique_Labels[m])[0]
		investment = Invest_Withdraw[where_Label[0]]
		withdrawal = Invest_Withdraw[where_Label[1]]
		this_ROI = (- withdrawal - investment) / float(investment)
		ROIs[unique_Labels[m]] = this_ROI
	
	####################
	# Initialize RealInvest dictionary:
	####################
	
	RI = {}
	
	####################
	# Calculate MinInvest dollar amount:
	####################
	
	MI = SB * MIpc
	
	####################
	# Initialize variables:
	####################
	
	TS = 0.	# tally slice
	c = 0	# slice count
	
	RT = np.zeros(N)
	BB = np.zeros(N)
	CT = np.zeros(N)
	P = np.zeros(N)
	
	####################
	# Start iterating through the transactions:
	####################
	
	for n in xrange(N):
		
		####################
		# Get appropriate values for BB and CT just before this transaction, and initialize addCT:
		####################
		
		if n==0:	# Then this is the first transaction:
			previous_BB = SB
			previous_CT = 0.
					
		else:		# Then this is not the first transaction:
			previous_BB = BB[n-1]
			previous_CT = CT[n-1]
		
		addCT = 0.	# The default to be added to CT is zero, it will only be positive when a profit is made during a withdrawal.
		
		####################
		# If this transaction is an investment:
		####################
			
		if Invest_Withdraw[n] > 0:
			
			if previous_BB >= MI:		# A RealTransact investment should only occur if the available BB is greater than or equal to MI.
				
				if c == s:	# Then there is no more compound tally left to spread onto the next transaction.
					TS = 0.
					
				else:
					c = c + 1	# Then we can still spread some compound tally, and the slice counter needs to be incremented.
				
				RT[n] = min(MI + TS, previous_BB)	# We can never invest more than what is available in the bank. 
				addCT = -1. * TS
				
				RI[Labels[n]] = RT[n]	# Insert this investment into the dictionary of current investments.
		
		####################
		# If this transaction is a withdrawal:
		####################
		
		else:
			
			if Labels[n] in RI:		# A RealTransact withdrawal should only occur if this label was actually invested in.
				
				RT[n] = - RI[Labels[n]] * (1 + ROIs[Labels[n]])
				
				if ROIs[Labels[n]] > 0:		# Then a profit was made:
					
					P[n] = RI[Labels[n]] * ROIs[Labels[n]]
					addCT =  MI + P[n]
					
					TS = (previous_CT + addCT) / float(s)		# Calculate new tally to be spread onto next transactions.
					c = 0
		
		####################
		# The BB and CT values are updated after each transaction:
		####################
		
		BB[n] = previous_BB - RT[n]
		CT[n] = previous_CT + addCT
		
	####################
	# Calculate and return net profit
	####################
	
	totalP = BB[N-1] - SB
	return totalP, RT, BB, CT

######################################################################
# Perform a single calculation, and save the output
######################################################################

def single_calculation():
	
	def callback1(SB, MIpc, s, Labels, Invest_Withdraw, Invest_Date, N):
		
		status_var.set('Calculating... Please wait...')
		
		TP, RT, BB, CT = compound_calculation(SB, MIpc, s, Labels, Invest_Withdraw, Invest_Date, N)
		
		####################
		# Write columns output file
		####################
		
		filename_identifier = str(int(SB)) + '_' + str(int(100*MIpc)) + '_' + str(s)
		
		columns_filename = output_dir + '/columns_' + filename_identifier + '_' + datetime.datetime.now().strftime("%Y%B%d_%H_%M_%S%p") + '.csv'
		columns_file = open(columns_filename, 'w')
		
		columns_file.write('Labels, InvestWithdraw, InvestDate, RealTransact, BankBalance, CompoundTally\n')
		
		for n in xrange(N):
			columns_file.write(Labels[n] + ',' + str(Invest_Withdraw[n]) + ',' + str(Invest_Date[n]) + ',' + str(round(RT[n],2)) + ',' + str(round(BB[n],2)) + ',' + str(round(CT[n],2)) + '\n')
		
		columns_file.close()
		
		####################
		# Write TP output file
		####################
		
		TP_filename = output_dir + '/TP_' + filename_identifier + '_' + datetime.datetime.now().strftime("%Y%B%d_%H_%M_%S%p") + '.csv'
		TP_file = open(TP_filename, 'w')
		
		TP_file.write(str(round(TP,2)) + '\n')
		
		TP_file.close()
		
		####################
		# Set program status to Ready
		####################
		
		status_var.set('Ready')
		
	####################
	# Read user input from entry boxes:
	####################
	
	global single_pars_good
	
	single_pars_good = True
	
	try:
		SB_value = float(singleSB_Entry.get())
		
		if SB_value <= 0:
			tkMessageBox.showerror("Starting Bank error", "The Starting Bank must be a float larger than zero.")
			single_pars_good = False
			
	except:
		tkMessageBox.showerror("Starting Bank error", "The Starting Bank must be a float larger than zero.")
		single_pars_good = False
	
	try:
		MIpc_value = float(singleMIpc_Entry.get()) / 100.
		
		if not 0 < MIpc_value <= 1:
			tkMessageBox.showerror("MinInvest % error", "The MinInvest % must be a float larger than 0, and no larger than 100.")
			single_pars_good = False
		
	except:
		tkMessageBox.showerror("MinInvest % error", "The MinInvest % must be a float larger than 0, and no larger than 100.")
		single_pars_good = False
		
	try:
		s_value = int(singleS_Entry.get())
		
		if not s_value > 0:
			tkMessageBox.showerror("Spread error", "The spread must have an integer value larger than zero.")
			single_pars_good = False
		
	except:
		tkMessageBox.showerror("Spread error", "The spread must have an integer value larger than zero.")
		single_pars_good = False
	
	if single_pars_good:
		
		####################
		# Read transactions from file:
		####################
		
		Labels_values, Invest_Withdraw_values, Invest_Date_values, N_value = read_transactions()
		
		if transactions_good:
			
			####################
			# Perform compound calculation using these parameters:
			####################
			
			thread1 = threading.Thread(target=callback1, args=(SB_value, MIpc_value, s_value, Labels_values, Invest_Withdraw_values, Invest_Date_values, N_value))
			thread1.start()

######################################################################
# Perform multiple iterations for optimization
######################################################################

def multiple_iterations():
	
	def callback2(SB, minMIpc, maxMIpc, stepMIpc, minS, maxS, stepS, Iter, risk, Labels, Invest_Withdraw, Invest_Date, N):
		
		status_var.set('Calculating... Please wait...')
		
		####################
		# Initialize grids and arrays:
		####################
		
		numMIpc = int((maxMIpc - minMIpc)/float(stepMIpc)) + 1
		real_maxMIpc = minMIpc + (numMIpc-1) * stepMIpc
		
		numS = int((maxS - minS)/float(stepS)) + 1
		real_maxS = minS + (numS-1) * stepS
		
		axis_MIpc = np.linspace(minMIpc, real_maxMIpc, num=numMIpc)
		axis_S = np.linspace(minS, real_maxS, num=numS, dtype=int)
		
		grid_MIpc, grid_S = np.meshgrid(axis_MIpc, axis_S, indexing='ij')
		
		TP = np.zeros((numMIpc, numS, Iter), dtype=np.float32)
		
		quartilesProfit = np.zeros((numMIpc, numS, 5), dtype=np.float32)	# This array will contain 5 values for each point on the grid:  minimum, first quartile, second quartile (median), third quartile and maximum.
		
		int_quart_range_Profit = np.zeros((numMIpc, numS), dtype=np.float32)
		rangeProfit = np.zeros((numMIpc, numS), dtype=np.float32)
		
		####################
		# Create a starting array from the transaction list:
		####################
		
		start_dtypes = [('labels', 'S100'), ('invest_withdraw', float), ('invest_date', 'datetime64[D]'), ('num', int), ('invest_type', 'S1')]
		start_values = []
		
		for n in xrange(N):
			
			if Invest_Withdraw[n] >=0:
				this_invest_type = 'i'
			else:
				this_invest_type = 'w'
			
			start_values.append((Labels[n], Invest_Withdraw[n], Invest_Date[n], 0, this_invest_type))
		
		start_arr = np.array(start_values, dtype=start_dtypes)
		
		####################
		# Calculate TP Iter times for each point on the grid:
		####################
		
		for k in xrange(Iter):
			
			shuffle_arr = np.array(start_arr)
			np.random.shuffle(shuffle_arr)
			
			for n in xrange(N):
				shuffle_arr['num'][n] = n
			
			sorted_arr = np.sort(shuffle_arr, order=['invest_date', 'invest_type', 'num'])
			
			for i in xrange(numMIpc):
				
				for j in xrange(numS):
					
					TP[i][j][k], dummyRT, dummyBB, dummyCT = compound_calculation(SB, axis_MIpc[i], axis_S[j], sorted_arr['labels'], sorted_arr['invest_withdraw'], sorted_arr['invest_date'], N)
		
		####################
		# Determine the profit minimum, first quartile, second quartile, third quartile and maximum, as well as interquartile range and range for each point on the grid:
		####################
		
		for i in xrange(numMIpc):
			
			for j in xrange(numS):
				
				int_quart_range_Profit[i][j] = np.percentile(TP[i][j], 75) - np.percentile(TP[i][j], 25)
				rangeProfit[i][j] = np.amax(TP[i][j]) - np.amin(TP[i][j])
				
				for k in xrange(5):
					
					the_percentile = k * 25
					quartilesProfit[i][j][k] = np.percentile(TP[i][j], the_percentile)
		
		####################
		# Order the results above in the preferred order according to the chosen risk, and write to output file:
		####################
		
		iter_dtypes = [('MI%', float), ('Spread', int), ('MinProfit', float), ('Q1Profit', float), ('Q2Profit', float), ('Q3Profit', float), ('MaxProfit', float), ('Int_quart_range_profit', float), ('Range_profit', float)]
		
		iter_values = []
		
		for i in xrange(numMIpc):
			
			for j in xrange(numS):
				
				iter_values.append((axis_MIpc[i], axis_S[j], quartilesProfit[i][j][0], quartilesProfit[i][j][1], quartilesProfit[i][j][2], quartilesProfit[i][j][3], quartilesProfit[i][j][4], int_quart_range_Profit[i][j], rangeProfit[i][j]))
		
		iter_arr = np.array(iter_values, dtype=iter_dtypes)
		whichProfit = iter_dtypes[1+risk][0]
		sort_iter_arr = np.sort(iter_arr, order=[whichProfit])
		
		iter_filename = output_dir + '/iterations_' + datetime.datetime.now().strftime("%Y%B%d_%H_%M_%S%p") + '.csv'
		
		iter_file = open(iter_filename, 'w')
		
		iter_file.write('MI%, Spread, MinProfit, Q1Profit, Q2Profit, Q3Profit, MaxProfit, Int_quart_range_profit, Range_profit\n')
		
		X = len(sort_iter_arr)
		
		for x in reversed(xrange(X)):	# Write to file in reversed order, since lowest profit is first entry in sort_iter_arr.
			
			iter_file.write(str(round(sort_iter_arr['MI%'][x]*100,2)) + ',' + str(sort_iter_arr['Spread'][x]) + ',' + str(round(sort_iter_arr['MinProfit'][x],2)) + ',' + str(round(sort_iter_arr['Q1Profit'][x],2)) + ',' + str(round(sort_iter_arr['Q2Profit'][x],2)) + ',' + str(round(sort_iter_arr['Q3Profit'][x],2)) + ',' + str(round(sort_iter_arr['MaxProfit'][x],2)) + ',' + str(round(sort_iter_arr['Int_quart_range_profit'][x],2)) + ',' + str(round(sort_iter_arr['Range_profit'][x],2)) + '\n')
		
		iter_file.close()
		
		####################
		# Save the optimized parameters (i.e. the last entry in sort_iter_arr) to another output file:
		####################
		
		optimized_filename = output_dir + '/optimized_' + datetime.datetime.now().strftime("%Y%B%d_%H_%M_%S%p") + '.csv'
		optimized_file = open(optimized_filename, 'w')
		
		optimized_file.write('SB, MI%_min, MI%_max, MI%_step, Spread_min, Spread_max, Spread_step, Iterations, Risk(1-5), Optimized_MI%, Optimized_Spread, Estimated_profit, Int_quart_range_profit, Range_profit\n')
		
		optimized_file.write(str(SB) + ',' + str(minMIpc*100) + ',' + str(maxMIpc*100) + ',' + str(stepMIpc*100) + ',' + str(minS) + ',' + str(maxS) + ',' + str(stepS) + ',' + str(Iter) + ',' + str(risk) + ',' + str(sort_iter_arr['MI%'][X-1]*100) + ',' + str(sort_iter_arr['Spread'][X-1]) + ',' + str(round(sort_iter_arr[whichProfit][X-1],2)) + ',' + str(round(sort_iter_arr['Int_quart_range_profit'][X-1],2)) + ',' + str(round(sort_iter_arr['Range_profit'][X-1],2)) + '\n')
		
		optimized_file.close()
		
		####################
		# Set program status to Ready
		####################
		
		status_var.set('Ready')
	
	global mult_pars_good
	mult_pars_good = True
	
	####################
	# Read SB from entry box:
	####################
	
	try:
		SB_value = float(SB_Entry.get())
		
		if SB_value <= 0:
			tkMessageBox.showerror("Starting Bank error", "The Starting Bank must be a float larger than zero.")
			mult_pars_good = False
		
	except:
		tkMessageBox.showerror("Starting Bank error", "The Starting Bank must be a float larger than zero.")
		mult_pars_good = False
	
	####################
	# Read MinInvest % parameters from entry boxes:
	####################
	
	if mult_pars_good:
		
		try:
			minMIpc_value = float(minMIpc_Entry.get()) / 100.
			
			if not 0 < minMIpc_value <= 1:
				tkMessageBox.showerror("Minimum MinInvest % error", "The minimum MinInvest % must be a float larger than 0, and no larger than 100.")
				mult_pars_good = False
			
		except:
			tkMessageBox.showerror("Minimum MinInvest % error", "The minimum MinInvest % must be a float larger than 0, and no larger than 100.")
			mult_pars_good = False
	
	if mult_pars_good:
		
		try:
			maxMIpc_value = float(maxMIpc_Entry.get()) / 100.
			
			if not minMIpc_value < maxMIpc_value <= 1:
				tkMessageBox.showerror("Maximum MinInvest % error", "The maximum MinInvest % must be a float larger than the minimum MinInvest %, and no larger than 100.")
				mult_pars_good = False
		
		except:
			tkMessageBox.showerror("Maximum MinInvest % error", "The maximum MinInvest % must be a float larger than the minimum MinInvest %, and no larger than 100.")
			mult_pars_good = False
	
	if mult_pars_good:
		
		try:
			stepMIpc_value = float(stepMIpc_Entry.get()) / 100.
			
			if stepMIpc_value <= 0 or stepMIpc_value > maxMIpc_value - minMIpc_value:
				tkMessageBox.showerror("MinInvest % step error", "The MinInvest % step must be larger than zero, and smaller than the difference between the maximum and minimum MinInvest % values.")
				mult_pars_good = False
			
		except:
			tkMessageBox.showerror("MinInvest % step error", "The MinInvest % step must be larger than zero, and smaller than the difference between the maximum and minimum MinInvest % values.")
			mult_pars_good = False
	
	####################
	# Read spread parameters from entry boxes:
	####################
	
	if mult_pars_good:
		
		try:
			minS_value = int(minS_Entry.get())
			
			if not minS_value > 0:
				tkMessageBox.showerror("Minimum spread error", "The minimum spread must have an integer value larger than zero.")
				mult_pars_good = False
			
		except:
			tkMessageBox.showerror("Minimum spread error", "The minimum spread must have an integer value larger than zero.")
			mult_pars_good = False
	
	if mult_pars_good:
	
		try:
			maxS_value = int(maxS_Entry.get())
			
			if not maxS_value > minS_value:
				tkMessageBox.showerror("Maximum spread error", "The maximum spread must have an integer value larger than the minimum spread.")
				mult_pars_good = False
			
		except:
			tkMessageBox.showerror("Maximum spread error", "The maximum spread must have an integer value larger than the minimum spread.")
			mult_pars_good = False
	
	if mult_pars_good:
		
		try:
			stepS_value = int(stepS_Entry.get())
			
			if stepS_value <= 0 or stepS_value > maxS_value - minS_value:
				tkMessageBox.showerror("Minimum spread error", "The spread step must be an integer larger than zero, and smaller than the difference between the maximum and minimum spread values.")
				mult_pars_good = False
			
		except:
			tkMessageBox.showerror("Minimum spread error", "The spread step must be larger than zero, and smaller than the difference between the maximum and minimum spread values.")
			mult_pars_good = False
	
	####################
	# Read number of iterations:
	####################
	
	if mult_pars_good:
		
		try:
			Iter_value = int(Iter_Entry.get())
			
			if not Iter_value > 0:
				tkMessageBox.showerror("Iterations error", "The number of iterations must have an integer value larger than zero.")
				mult_pars_good = False
			
		except:
			tkMessageBox.showerror("Iterations error", "The number of iterations must have an integer value larger than zero.")
			mult_pars_good = False
	
	####################
	# Read risk level:
	####################
	
	if mult_pars_good:
		
		try:
			risk_value = int(risk_Entry.get())
			
			if not 1 <= risk_value <= 5:
				tkMessageBox.showerror("Risk error", "The risk level must have an integer value from 1 to 5.")
				mult_pars_good = False
			
		except:
			tkMessageBox.showerror("Risk error", "The risk level must have an integer value from 1 to 5.")
			mult_pars_good = False
	
	if mult_pars_good:
		
		####################
		# Read transactions from file:
		####################
		
		Labels_values, Invest_Withdraw_values, Invest_Date_values, N_value = read_transactions()
		
		if transactions_good:
				
			thread2 = threading.Thread(target=callback2, args=(SB_value, minMIpc_value, maxMIpc_value, stepMIpc_value, minS_value, maxS_value, stepS_value, Iter_value, risk_value, Labels_values, Invest_Withdraw_values, Invest_Date_values, N_value))
			thread2.start()

######################################################################
# MAIN PROGRAM
######################################################################

####################
# Declare global variables
####################

global transact_filename_var; global output_dir_var; global status_var

global transactions_good; global single_pars_good; global mult_pars_good

transactions_good = False
single_pars_good = False
mult_pars_good = False

root = tk.Tk()
root.wm_title('Compound Calculator v1.2')
 
transact_filename_var = tk.StringVar()

output_dir_var = tk.StringVar()

status_var = tk.StringVar()

status_var.set('Ready')

####################
# GUI for transaction file selection
####################

transactFrame = tk.Frame(root, height=110, width=650, relief=tk.RIDGE, bd=3)
transactFrame.pack_propagate(False)
transactFrame.grid(row=0, columnspan=2, padx=15, pady=15)

UploadButton = tk.Button(transactFrame, text="Select transaction file", font="Helvetica 14 bold", command=select_file)
UploadButton.pack(side='top')

transact_heading_Label = tk.Label(transactFrame, text="You have selected the following file:", padx=10, pady=10, font="Helvetica 11")
transact_heading_Label.pack(side='top')

transactLabel = tk.Label(transactFrame, textvariable=transact_filename_var, font="Helvetica 10", anchor='e')
transactLabel.pack(side='top', fill='x')

####################
# GUI for setting output directory
####################

outputFrame = tk.Frame(root, height=110, width=650, relief=tk.RIDGE, bd=3)
outputFrame.pack_propagate(False)
outputFrame.config(highlightbackground="black")
outputFrame.grid(row=1, columnspan=2, padx=15, pady=15)

outdirButton = tk.Button(outputFrame, text="Set output directory", font="Helvetica 14 bold", relief=tk.RAISED, command=set_output_dir)
outdirButton.pack(side='top')

outdir_heading_Label = tk.Label(outputFrame, text="You have set the following output directory:", padx=10, pady=10, font="Helvetica 11")
outdir_heading_Label.pack(side='top')

outdirLabel = tk.Label(outputFrame, textvariable=output_dir_var, font="Helvetica 10", anchor='e')
outdirLabel.pack(side='top', fill='x')

####################
# GUI for user input - multiple calculations
####################

multCalcFrame = tk.Frame(root, height=285, width=310, relief=tk.RIDGE, bd=3)
multCalcFrame.grid_propagate(False)
multCalcFrame.grid(row=2, column=0, rowspan=2, padx=15, pady=15)

multCalc_heading_Label = tk.Label(multCalcFrame, text="Run multiple iterations", padx=10, pady=10, font="Helvetica 14 bold")
multCalc_heading_Label.grid(row=0, columnspan=2, column=1)

SB_Label = tk.Label(multCalcFrame, text="Starting Bank:")
SB_Label.grid(row=1, sticky="E", column=1)

MIpc_Label = tk.Label(multCalcFrame, text="MinInvest % (min):")
MIpc_Label.grid(row=2, sticky="E", column=1)

MIpc_Label = tk.Label(multCalcFrame, text="MinInvest % (max):")
MIpc_Label.grid(row=3, sticky="E", column=1)

MIpc_Label = tk.Label(multCalcFrame, text="MinInvest % (step):")
MIpc_Label.grid(row=4, sticky="E", column=1)

S_Label = tk.Label(multCalcFrame, text="Spread (min):")
S_Label.grid(row=5, sticky="E", column=1)

S_Label = tk.Label(multCalcFrame, text="Spread (max):")
S_Label.grid(row=6, sticky="E", column=1)

S_Label = tk.Label(multCalcFrame, text="Spread (step):")
S_Label.grid(row=7, sticky="E", column=1)

Iter_Label = tk.Label(multCalcFrame, text="Iterations:")
Iter_Label.grid(row=8, sticky="E", column=1)

risk_Label = tk.Label(multCalcFrame, text="Risk level (1 to 5):")
risk_Label.grid(row=9, sticky="E", column=1)

SB_Entry = tk.Entry(multCalcFrame)
SB_Entry.grid(row=1, column=2)

minMIpc_Entry = tk.Entry(multCalcFrame)
minMIpc_Entry.grid(row=2, column=2)

maxMIpc_Entry = tk.Entry(multCalcFrame)
maxMIpc_Entry.grid(row=3, column=2)

stepMIpc_Entry = tk.Entry(multCalcFrame)
stepMIpc_Entry.grid(row=4, column=2)

minS_Entry = tk.Entry(multCalcFrame)
minS_Entry.grid(row=5, column=2)

maxS_Entry = tk.Entry(multCalcFrame)
maxS_Entry.grid(row=6, column=2)

stepS_Entry = tk.Entry(multCalcFrame)
stepS_Entry.grid(row=7, column=2)

Iter_Entry = tk.Entry(multCalcFrame)
Iter_Entry.grid(row=8, column=2)

risk_Entry = tk.Entry(multCalcFrame)
risk_Entry.grid(row=9, column=2)

multCalcButton = tk.Button(multCalcFrame, text="Calculate and save", font="Helvetica 11 bold", command=multiple_iterations)
multCalcButton.grid(row=10, columnspan=2, column=1)

multCalcFrame.grid_columnconfigure(0, weight=1)
multCalcFrame.grid_columnconfigure(3, weight=1)

####################
# GUI for user input - single calculation
####################

singleCalcFrame = tk.Frame(root, height=160, width=310, relief=tk.RIDGE, bd=3)
singleCalcFrame.grid_propagate(False)
singleCalcFrame.grid(row=2, column=1, padx=15, pady=15)

singleCalc_heading_Label = tk.Label(singleCalcFrame, text="Run a single calculation", padx=10, pady=10, font="Helvetica 14 bold")
singleCalc_heading_Label.grid(row=0, columnspan=2, column=1)

singleSB_Label = tk.Label(singleCalcFrame, text="Starting Bank:")
singleMIpc_Label = tk.Label(singleCalcFrame, text="MinInvest %:")
singleS_Label = tk.Label(singleCalcFrame, text="Spread:")

singleSB_Entry = tk.Entry(singleCalcFrame)
singleMIpc_Entry = tk.Entry(singleCalcFrame)
singleS_Entry = tk.Entry(singleCalcFrame)

singleSB_Label.grid(row=1, sticky="E", column=1)
singleMIpc_Label.grid(row=2, sticky="E", column=1)
singleS_Label.grid(row=3, sticky="E", column=1)

singleSB_Entry.grid(row=1, column=2)
singleMIpc_Entry.grid(row=2, column=2)
singleS_Entry.grid(row=3, column=2)

singleCalcButton = tk.Button(singleCalcFrame, text="Calculate and save", font="Helvetica 11 bold", command=single_calculation)
singleCalcButton.grid(row=4, columnspan=2, column=1)

singleCalcFrame.grid_columnconfigure(0, weight=1)
singleCalcFrame.grid_columnconfigure(3, weight=1)

####################
# GUI for status
####################

statusFrame = tk.Frame(root, height=95, width=310, relief=tk.RIDGE, bd=3)
statusFrame.pack_propagate(False)
statusFrame.grid(row=3, column=1, padx=15, pady=15)

status_heading_Label = tk.Label(statusFrame, text="Program status", padx=10, pady=10, font="Helvetica 14 bold")
status_heading_Label.pack(side='top')

statusLabel = tk.Label(statusFrame, textvariable=status_var, font="Helvetica 14", fg='red')
statusLabel.pack(side='top', fill='x')

######################################################################

root.mainloop()

######################################################################

quit()
