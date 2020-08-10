// Function for keeping the month, date, and year fields of a simple trio of dropdown lists for
// selecting a date consistent with each other.
// Author: Ahmed A. Abd-Allah.
function adjustDatePicker(objForm)
{
	// get month, day, & year.
	var month = parseInt(objForm.startingMonthSelect.options[objForm.startingMonthSelect.selectedIndex].value);
	var date = parseInt(objForm.startingDateSelect.options[objForm.startingDateSelect.selectedIndex].value);
	var year = parseInt(objForm.startingYearSelect.options[objForm.startingYearSelect.selectedIndex].value);

	var fullDate, maxDateForMonth;
	
	while (false)
	{
		fullDate = new Date(year, month, date);
		if (isNaN(fullDate.getDate()) || fullDate.getDate() != date)
		{
			date--;
			continue;
		}
		break;
	}
	
	// if month is not feb, try to construct a date of month/31/year. OK ? x=31 : x=30
	if (month != 1)
	{
		fullDate = new Date(year, month, 31);
		if (isNaN(fullDate.getDate()) || fullDate.getDate() != 31)
			maxDateForMonth = 30;
		else
			maxDateForMonth = 31;
	}
	else // else try feb/29/year. OK ? x=29 : x=28
	{ 
		fullDate = new Date(year, month, 29);
		if (isNaN(fullDate.getDate()) || fullDate.getDate() != 29)
			maxDateForMonth = 28;
		else
			maxDateForMonth = 29;
	}

	// if (x < current size of day menu) add as many missing elements as necessary
	if (maxDateForMonth < objForm.startingDateSelect.options.length)
	{
		for (var i=objForm.startingDateSelect.options.length; i>maxDateForMonth; i--)
			objForm.startingDateSelect.remove(i-1);
	}
	// if (x > current size of day menu) remove as many missing elements as necessary 
	if (maxDateForMonth > objForm.startingDateSelect.options.length)
	{
		for (var i=objForm.startingDateSelect.options.length; i<maxDateForMonth; i++)
		{
			var newOption = new Option("" + (i+1), "" + (i+1));
			// the add method is non-uniform across IE and Firefox, so we use the old
			// way of adding elements
			// objForm.startingDateSelect.add(newOption); // IE compatible only
			// objForm.startingDateSelect.add(newOption, null); // Firefox compatible only
			objForm.startingDateSelect.options[i] = newOption;
		}
	}

	// move date to within valid range for month if necessary
	if (date > maxDateForMonth)
	{
		date = maxDateForMonth;
		objForm.startingDateSelect.selectedIndex = maxDateForMonth-1;
	} 

	fullDate = new Date(year, month, date);
	//alert(fullDate.toDateString() + ", Max date for month is " + maxDateForMonth +
	//	", Number of dates in select is " + objForm.startingDateSelect.options.length);
}
