import React from 'react';
import PropTypes from 'prop-types';
import 'react-widgets/dist/css/react-widgets.css';
import DateTimePicker from 'react-widgets/lib/DateTimePicker';
import DropdownList from 'react-widgets/lib/DropdownList';
import dateFnsLocalizer from 'react-widgets-date-fns';
import { format } from 'date-fns-tz';
import { listTimeZones } from 'timezone-support';
import { Redirect } from 'react-router-dom';
import Spinner from 'react-bootstrap/Spinner';
import Alert from 'react-bootstrap/Alert';
import { makeFsm } from './utils';

dateFnsLocalizer();

const defaultTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
const dropdownTimeZones = listTimeZones().map(tz => ({
  timeZone: tz,
  humanTimeZone: tz.replace(/_/g, ' '),
}));

const FsmState = makeFsm('Displaying', 'Submitting', 'Succeeding', 'Failing');

export default class EventEditor extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      title: '',
      dateTime: '',
      timeZone: defaultTimeZone,
      description: '',

      fsmState: FsmState.Displaying,
    };

    this.handleInputChange = this.handleInputChange.bind(this);
    this.handleDateTimeChange = this.handleDateTimeChange.bind(this);
    this.handleTimeZoneChange = this.handleTimeZoneChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentDidMount() {
    // the DropdownList doesn't let us customize the input component (and interprets the given `id` prop as name...)
    const input = document.querySelector('#timeZone_input input');
    input.id = 'timeZone';
    input.required = 'required';
  }

  handleInputChange(event) {
    const { target } = event;
    const { value, name } = target;

    this.setState({
      [name]: value,
    });
  }

  handleTimeZoneChange(dropdownTimeZone) {
    this.setState({
      timeZone: dropdownTimeZone.timeZone,
    });
  }

  handleDateTimeChange(dateTime) {
    this.setState({
      dateTime,
    });
  }

  handleSubmit(event) {
    event.preventDefault();
    this.setState({ fsmState: FsmState.Submitting });
    this.props.eventRepository
      .create({
        id: this.state.id,
        hostId: this.state.hostId,
        title: this.state.title,
        description: this.state.description,
        dateTime: `${format(this.state.dateTime, "yyyy-MM-dd'T'HH:mmXXX", {
          timeZone: this.state.timeZone,
          convertTimeZone: false,
        })}[${this.state.timeZone}]`,
      })
      .then(() => {
        this.setState({ fsmState: FsmState.Succeeding });
        this.props.setToast({
          header: this.state.title,
          body: 'Your event has been created.',
        });
      })
      .catch(() => {
        this.setState({ fsmState: FsmState.Failing });
      });
  }

  renderSucceeding() {
    return <Redirect push to="/" />;
  }

  renderForm() {
    return (
      <>
        <h2>Creating Event</h2>
        {this.state.fsmState.isFailing && <Alert variant="danger">Could not create event.</Alert>}
        <form method="post" onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="title">Title</label>
            <input
              className="form-control"
              id="title"
              name="title"
              value={this.state.title}
              onChange={this.handleInputChange}
              required
            />
          </div>

          <div className="form-row align-items-center form-group">
            <div className="col">
              <label htmlFor="dateTime_input">Date &amp; Time</label>
              <DateTimePicker
                id="dateTime"
                name="dateTime"
                onChange={this.handleDateTimeChange}
                inputProps={{ required: 'requried' }}
              />
            </div>

            <div className="col">
              <label htmlFor="timeZone">Time Zone</label>
              <DropdownList
                id="timeZone"
                name="timeZone"
                onChange={this.handleTimeZoneChange}
                data={dropdownTimeZones}
                textField="humanTimeZone"
                valueField="timeZone"
                filter="contains"
                defaultValue={Intl.DateTimeFormat().resolvedOptions().timeZone}
                required=""
              />
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
              <button type="button" className="btn btn-outline-secondary form-control" id="addGuestEmail">
                Add External Guest
              </button>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea
              className="form-control"
              id="description"
              name="description"
              value={this.state.description}
              onChange={this.handleInputChange}
            />
          </div>

          <input
            type="text"
            id="hiddenDateTime"
            name="hidden"
            value={this.state.dateTime}
            style={{ display: 'none' }}
            required
            readOnly
          />
          <input
            type="text"
            id="hiddenTimeZone"
            name="hidden"
            value={this.state.timeZone}
            style={{ display: 'none' }}
            required
            readOnly
          />

          <button id="createEvent" className="btn btn-primary" type="submit">
            {this.state.fsmState.isSubmitting && (
              <Spinner
                as="span"
                animation="border"
                size="sm"
                role="status"
                aria-hidden="true"
                style={{ marginRight: '.5em' }}
              />
            )}
            Create Event
          </button>
        </form>
      </>
    );
  }

  render() {
    if (this.state.fsmState.isSucceeding) return this.renderSucceeding();
    return this.renderForm();
  }
}

EventEditor.propTypes = {
  setToast: PropTypes.func.isRequired,
  eventRepository: PropTypes.shape({
    create: PropTypes.func.isRequired,
  }).isRequired,
};
