import React from 'react';
import 'react-widgets/dist/css/react-widgets.css';
import DateTimePicker from 'react-widgets/lib/DateTimePicker';
import DropdownList from 'react-widgets/lib/DropdownList'
import dateFnsLocalizer from 'react-widgets-date-fns';
import { format } from 'date-fns-tz';
import { listTimeZones } from 'timezone-support';
import { Redirect } from "react-router-dom";

dateFnsLocalizer();

const defaultTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone
const dropdownTimeZones = listTimeZones().map(tz => ({ timeZone: tz, humanTimeZone: tz.replace(/_/g, ' ') }))

export default class EventEditor extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      id: props.generateUuid(),
      hostId: '6f31ccde-4321-4cc9-9056-6c3cbd550cba',

      title: '',
      dateTime: '',
      timeZone: defaultTimeZone,
      description: '',

      submitted: false
    };

    this.handleInputChange = this.handleInputChange.bind(this);
    this.handleDateTimeChange = this.handleDateTimeChange.bind(this);
    this.handleTimeZoneChange = this.handleTimeZoneChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleInputChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;

    this.setState({
      [name]: value
    });
  }

  handleTimeZoneChange(dropdownTimeZone) {
    this.setState({
      timeZone: dropdownTimeZone.timeZone
    })
  }

  handleDateTimeChange(dateTime) {
    this.setState({
      dateTime
    })
  }

  handleSubmit(event) {
    event.preventDefault();

    fetch(process.env.REACT_APP_API_URL + '/events', {
      method: 'POST',
      body: JSON.stringify({
        id: this.state.id,
        hostId: this.state.hostId,
        title: this.state.title,
        description: this.state.description,
        dateTime: format(this.state.dateTime, "yyyy-MM-dd'T'HH:mmXXX", { timeZone: this.state.timeZone, convertTimeZone: false }) + '[' + this.state.timeZone + ']'
      })
    }).then(() => {
      this.setState({ submitted: true });
      this.props.setToast({ header: this.state.title, body: 'Your event has been created.' });
    });
  }

  componentDidMount() {
    // the DropdownList doesn't let us customize the input component (and interprets the given `id` prop as name...)
    const input = document.querySelector('#timeZone_input input')
    input.id = 'timeZone';
    input.required = "required";
  }

  render() {
    if (this.state.submitted) return this.renderSubmitted(); else return this.renderForm();
  }

  renderSubmitted() {
    return <Redirect push to="/" />;
  }

  renderForm() {
    return (
      <>
        <h2>Creating Event</h2>
        <form method="post" onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="title">Title</label>
            <input className="form-control" id="title" name="title" value={this.state.title} onChange={this.handleInputChange} required />
          </div>

          <div className="form-row align-items-center form-group">
            <div className="col">
              <label htmlFor="dateTime_input">Date &amp; Time</label>
              <DateTimePicker id="dateTime" name="dateTime" onChange={this.handleDateTimeChange} inputProps={{ required: "requried" }} />
            </div>

            <div className="col">
              <label htmlFor="timeZone">Time Zone</label>
              <DropdownList id="timeZone" name="timeZone" onChange={this.handleTimeZoneChange} data={dropdownTimeZones} textField='humanTimeZone' valueField='timeZone' filter='contains' defaultValue={Intl.DateTimeFormat().resolvedOptions().timeZone} required="" />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="guestIds">Guests</label>
            <select className="form-control" id="guestIds" name="guestIds[]" multiple>
              <option disabled>Select Guests</option>
              <option value="id">Guest Name</option>
            </select>
          </div>

          <div className="form-row align-items-center form-group">
            <div className="col">
              <label htmlFor="guestName">External Guest Name</label>
              <input className="form-control" id="guestName" />
            </div>
            <div className="col">
              <label htmlFor="guestEmail">External Guest Email</label>
              <input className="form-control" id="guestEmail" type="email" />
            </div>
            <div className="col">
              <label>&nbsp;</label>
              <button className="btn btn-outline-secondary form-control" id="addGuestEmail">Add External Guest</button>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea className="form-control" id="description" name="description" value={this.state.description} onChange={this.handleInputChange} />
          </div>

          <input type="text" id="hiddenDateTime" name="hidden" value={this.state.dateTime} style={{ display: 'none' }} required readOnly />
          <input type="text" id="hiddenTimeZone" name="hidden" value={this.state.timeZone} style={{ display: 'none' }} required readOnly />

          <input id="createEvent" className="btn btn-primary" type="submit" value="Create Event" />
        </form>
      </>
    );
  }
};